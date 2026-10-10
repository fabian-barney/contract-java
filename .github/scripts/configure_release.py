"""Idempotent repository setup, run by a maintainer outside publication CI."""

from release_common import REPOSITORY, gh, require


def configure() -> None:
    branch = gh(f"repos/{REPOSITORY}/branches/main")
    require(branch["protected"], "main must already be protected")
    protection = gh(
        f"repos/{REPOSITORY}/branches/main/protection/required_status_checks"
    )
    contexts = sorted(set(protection["contexts"]) | {"verify / required"})
    gh(
        f"repos/{REPOSITORY}/branches/main/protection/required_status_checks",
        "PATCH",
        {"strict": True, "contexts": contexts},
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
        "Required aggregator, protected-branch environments, Actions Pages, and immutable tags configured."
    )


if __name__ == "__main__":
    configure()
