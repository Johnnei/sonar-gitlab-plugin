# Unreleased

# 0.1.0-rc.2 (2016-12-31)
## Security Changes
- [SGP-27](https://jira.johnnei.org/browse/SGP-27): Prefer usage of GitLab access tokens.
- [SGP-28](https://jira.johnnei.org/browse/SGP-28): Mark auth token as password field and documentate security risks.

## Bug Fixes
- [SGP-29](https://jira.johnnei.org/browse/SGP-29): Only trigger post issue job when the commit hash is supplied.

# 0.1.0-rc.1 (2016-12-30)
## New Features
- [SGP-1](https://jira.johnnei.org/browse/SGP-1): Create comments in GitLab on commits.
- [SGP-2](https://jira.johnnei.org/browse/SGP-2): Don't duplicate comments on incremental analyses.
- [SGP-4](https://jira.johnnei.org/browse/SGP-4): Create summary comment in GitLab on commit.

## Compatibility Changes
- [SGP-3](https://jira.johnnei.org/browse/SGP-3): Ensure compatibility with SonarQube LTS through 6.2 and GitLab 8.12 through 8.14.
- [SGP-8](https://jira.johnnei.org/browse/SGP-4): Ensure compatibility with GitLab 8.15. Drops validated support for 8.12.
