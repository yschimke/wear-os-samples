# Material3 Sample Patches

Place optional `*.patch` files in this directory to fix imported sample sources after extraction.
`scripts/import-material3-samples.sh` applies them in shell glob order from `app/src/samples`.

Generate patches relative to `app/src/samples`. For example:

```sh
git diff -- app/src/samples > patches/material3-samples/0001-fix-compile.patch
```

There are currently no source patches required for the pinned AndroidX revision.
