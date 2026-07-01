#!/usr/bin/env bash
#
# fund-vault-e2e.sh — run the Vault on-chain e2e on localnet, servicing the
# device→host funding requests the test logs.
#
# The instrumented test (VaultDeployE2ETest) logs, per fresh wallet:
#   KUIRA_FUND_REQ addr=<bech32m> night=<int> small=<int> dust=<true|false>
# This script tails logcat for those and runs `mn airdrop <night> --wallet <addr>`
# (deduped by addr). Dust registers ON-DEVICE (a keyed tx the host can't sign).
#
# Prereqs: an emulator running, localnet up (`mn localnet start`), `mn` + `adb` in PATH.
# Uses --refresh-dependencies so the app picks up a freshly republished alpha05 SDK.
set -uo pipefail

cd "$(dirname "$0")"

ADB="adb ${ANDROID_SERIAL:+-s $ANDROID_SERIAL}"

command -v adb >/dev/null 2>&1 || { echo "✗ adb not in PATH" >&2; exit 1; }
$ADB devices | grep -qE "device$|emulator-" || { echo "✗ No adb device. Start an emulator first." >&2; exit 1; }
command -v mn  >/dev/null 2>&1 || { echo "✗ mn CLI not in PATH (needed for funding)" >&2; exit 1; }
# Localnet liveness = the node JSON-RPC port answering (more reliable than a docker
# container-name grep, which varies by how localnet was started).
nc -z 127.0.0.1 9944 2>/dev/null || { echo "✗ Localnet node (127.0.0.1:9944) not reachable. Start it with: mn localnet start" >&2; exit 1; }
echo "  ✓ adb device, mn CLI, localnet reachable"

CLASS="${VAULT_E2E_CLASS:-com.kuiralabs.starter.counter.VaultDeployE2ETest}"

$ADB logcat -c

echo "── Running :app:connectedDebugAndroidTest ($CLASS) ──"
./gradlew :app:connectedDebugAndroidTest --refresh-dependencies \
  -Pandroid.testInstrumentationRunnerArguments.class="$CLASS" \
  > /tmp/vault-e2e-test.log 2>&1 &
TEST_PID=$!

echo "── Funding servicer: watching for KUIRA_FUND_REQ ──"
$ADB logcat -s KuiraE2EFund:* > /tmp/vault-e2e-fund.log 2>&1 &
LOGCAT_PID=$!

# File-based dedup (macOS ships bash 3.2 — no `declare -A` associative arrays).
SEEN_FILE="$(mktemp)"
trap 'rm -f "$SEEN_FILE"' EXIT
while kill -0 "$TEST_PID" 2>/dev/null; do
    while IFS= read -r line; do
        case "$line" in *KUIRA_FUND_REQ*) : ;; *) continue ;; esac
        addr=$(echo "$line"  | sed -nE 's/.*addr=([^ ]+).*/\1/p')
        night=$(echo "$line" | sed -nE 's/.*night=([0-9]+).*/\1/p')
        small=$(echo "$line" | sed -nE 's/.*small=([0-9]+).*/\1/p')
        [ -z "$addr" ] && continue
        grep -qxF "$addr" "$SEEN_FILE" 2>/dev/null && continue
        echo "$addr" >> "$SEEN_FILE"
        small="${small:-1}"; [ "$small" -lt 1 ] && small=1
        per=$(( ${night:-0} / small ))
        echo "  -> airdrop $addr : ${per} NIGHT x${small}  (dust registered on-device)"
        for _ in $(seq 1 "$small"); do mn airdrop "$per" --wallet "$addr" >/dev/null 2>&1 || true; done
    done < /tmp/vault-e2e-fund.log
    sleep 2
done

wait "$TEST_PID"; RC=$?
kill "$LOGCAT_PID" 2>/dev/null || true

echo "=== test result (rc=$RC) ==="
grep -iE "Tests run|FAILED|OK \(|deploy stage|deployed at|AssumptionViolated|BUILD (SUCCESSFUL|FAILED)" /tmp/vault-e2e-test.log | tail -25
exit $RC
