curl \
  -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: Bearer GIT_HUB_TOKEN" \
  https://api.github.com/repos/manturovDan/monorepo_simulator_5/pulls \
  -d "{\"title\":\"PR $1\",\"body\":\"PR $1!\",\"head\":\"branch_$1\",\"base\":\"main\"}"