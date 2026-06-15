#!/usr/bin/env bash
# Load .env without bash interpreting &, ?, spaces, etc. in values.
# Usage: source scripts/load-env.sh   (from repo root)

_ENV_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
_ENV_FILE="${ENV_FILE:-$_ENV_ROOT/.env}"

if [[ ! -f "$_ENV_FILE" ]]; then
  return 0 2>/dev/null || exit 0
fi

while IFS= read -r _line || [[ -n "$_line" ]]; do
  [[ "$_line" =~ ^[[:space:]]*# ]] && continue
  _line="${_line#"${_line%%[![:space:]]*}"}"
  _line="${_line%"${_line##*[![:space:]]}"}"
  [[ -z "$_line" ]] && continue
  [[ "$_line" != *"="* ]] && continue

  _key="${_line%%=*}"
  _value="${_line#*=}"
  _key="${_key%"${_key##*[![:space:]]}"}"
  _key="${_key#"${_key%%[![:space:]]*}"}"

  if [[ "$_value" == \"*\" ]]; then
    _value="${_value#\"}"
    _value="${_value%\"}"
  elif [[ "$_value" == \'*\' ]]; then
    _value="${_value#\'}"
    _value="${_value%\'}"
  fi

  export "$_key=$_value"
done < "$_ENV_FILE"