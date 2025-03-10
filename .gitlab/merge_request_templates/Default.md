## What does this merge request do?

TODO Link associated issue from title, like: `<title> #NUMBER`

TODO Briefly list what this merge request is about

## Author's checklist

- [ ] This merge request fully addresses the requirements of the associated task
- [ ] I did a self-review of the changes and did not spot any issues, among others:
  - I added unit tests for new or changed behavior; existing and new tests pass
  - My code conforms to our coding standards and guidelines
  - My changes are prepared (focused commits, good messages) so reviewing them is easy for the reviewer
- [ ] I amended the [changelog](/CHANGELOG.md) if this affects users in any way
- [ ] I assigned a reviewer to request review

## Reviewer's checklist

- [ ] I reviewed all changes line-by-line and addressed relevant issues. However:
  - for quickly resolved issues, I considered creating a fixup commit and discussing that, and
  - instead of many or long comments, I considered a meeting with or a draft commit for the author.
- [ ] The requirements of the associated task are fully met
- [ ] I can confirm that:
  - CI passes
  - If applicable, coverage percentages do not decrease
  - New code conforms to standards and guidelines
  - If applicable, additional checks were done for special code changes (e.g. core performance, binary size, OSS licenses)
