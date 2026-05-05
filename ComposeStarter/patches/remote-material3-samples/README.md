# Remote Material3 Sample Patches

Place optional `*.patch` files in this directory to fix imported sample sources after extraction.
`scripts/import-remote-material3-samples.sh` applies them in shell glob order with:

```sh
git apply --directory=app/src/samples patches/remote-material3-samples/*.patch
```

Generate patches relative to `app/src/samples`. For example:

```sh
git diff -- app/src/samples > patches/remote-material3-samples/0001-fix-compile.patch
```

There are currently no source patches required for the pinned AndroidX revision.
