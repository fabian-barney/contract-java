#!/usr/bin/env bash
# Git's GPG callback: secrets stay out of arguments and files.
set -euo pipefail
set +x
test -n "${MAVEN_GPG_PASSPHRASE:-}"
passphrase="$MAVEN_GPG_PASSPHRASE"
unset MAVEN_GPG_PASSPHRASE
time_args=()
if [[ -n "${RELEASE_EPOCH:-}" ]]; then
  time_args=(--faked-system-time "${RELEASE_EPOCH}!")
fi
exec gpg --batch --no-tty --pinentry-mode loopback --passphrase-fd 3 "${time_args[@]}" "$@" 3< <(printf '%s\n' "$passphrase")
