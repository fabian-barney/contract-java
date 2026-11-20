"""Idempotent repository setup, run by a maintainer outside publication CI.

Release branches take their protection from the classic ``release/**/*`` rule
that mirrors ``main``. This script verifies that the 2.x release branch requires
the CI aggregator, then configures the shared release settings.
"""

from urllib.parse import quote

from release_common import RELEASE_BRANCH, REPOSITORY, gh, require

REQUIRED_CHECK = "verify / required"


def configure() -> None:
    branch = gh(f"repos/{REPOSITORY}/branches/{quote(RELEASE_BRANCH, safe='')}")
    checks = branch.get("protection", {}).get("required_status_checks", {})
    require(
        branch["protected"]
        and checks.get("enforcement_level") == "everyone"
        and REQUIRED_CHECK in checks.get("contexts", []),
        f"{RELEASE_BRANCH} must require {REQUIRED_CHECK} for everyone",
    )
    for environment in ("release", "github-pages"):
        gh(
            f"repos/{REPOSITORY}/environments/{environment}",
            "PUT",
            {
                "wait_timer": 0,
                "reviewers": [],
                "can_admins_bypass": False,
                "deployment_branch_policy": {
                    "protected_branches": True,
                    "custom_branch_policies": False,
                },
            },
        )
    gh(f"repos/{REPOSITORY}/pages", "PUT", {"build_type": "workflow"})
    rulesets = gh(f"repos/{REPOSITORY}/rulesets")
    if not any(rule["name"] == "immutable release tags" for rule in rulesets):
        gh(
            f"repos/{REPOSITORY}/rulesets",
            "POST",
            {
                "name": "immutable release tags",
                "target": "tag",
                "enforcement": "active",
                "conditions": {
                    "ref_name": {"include": ["refs/tags/v*"], "exclude": []}
                },
                "rules": [{"type": "deletion"}, {"type": "non_fast_forward"}],
                "bypass_actors": [],
            },
        )
    print(
        "Release-branch checks verified; protected-branch environments, Actions Pages, and immutable tags configured."
    )


if __name__ == "__main__":
    configure()
