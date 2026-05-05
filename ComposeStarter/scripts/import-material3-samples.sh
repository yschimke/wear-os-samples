#!/usr/bin/env bash
set -euo pipefail

ANDROIDX_SHA="${ANDROIDX_SHA:-115471226870b0fddaeee5962f91ffcbe080ed74}"
ANDROIDX_ARCHIVE_URL="${ANDROIDX_ARCHIVE_URL:-https://github.com/androidx/androidx/archive/${ANDROIDX_SHA}.tar.gz}"
SOURCE_SUBDIR="wear/compose/compose-material3/samples/src/main"
DEST_DIR="app/src/samples"
PATCH_DIR="patches/material3-samples"

tmp_dir="$(mktemp -d)"
archive="${tmp_dir}/androidx-${ANDROIDX_SHA}.tar.gz"
extract_dir="${tmp_dir}/extract"

cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

mkdir -p "${extract_dir}" "${DEST_DIR}"

echo "Downloading ${ANDROIDX_ARCHIVE_URL}"
curl -L --fail --show-error --output "${archive}" "${ANDROIDX_ARCHIVE_URL}"

echo "Extracting ${SOURCE_SUBDIR} to ${DEST_DIR}"
find "${DEST_DIR}" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
tar -xzf "${archive}" \
  -C "${extract_dir}" \
  --strip-components=7 \
  "androidx-${ANDROIDX_SHA}/${SOURCE_SUBDIR}"

cp -R "${extract_dir}/." "${DEST_DIR}/"

if compgen -G "${PATCH_DIR}/*.patch" > /dev/null; then
  echo "Applying patches from ${PATCH_DIR}"
  for patch in "${PATCH_DIR}"/*.patch; do
    echo "Applying ${patch}"
    patch --no-backup-if-mismatch -p1 --directory "${DEST_DIR}" --input "../../../${patch}"
  done
fi

echo "Imported Material3 samples into ${DEST_DIR}"
