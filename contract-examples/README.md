# contract-examples

This module contains test-scoped examples that compile and run in the normal
reactor build. It is intentionally skipped during Maven deploy so the examples
stay useful as CI coverage without becoming a published artifact.

Run the examples with:

```shell
./mvnw -pl contract-examples -am test
```

The tests demonstrate built-in contracts, a custom composed contract annotation,
and masked violation messages for confidential values.
