#!/bin/bash

set -e

message="{\"text\": \"*${BUILD_STATUS}*: build <${CIRCLE_BUILD_URL}|#${CIRCLE_BUILD_NUM}> in *${CIRCLE_PROJECT_REPONAME}* (_${CIRCLE_BRANCH}_:<${CIRCLE_COMPARE_URL}|${CIRCLE_SHA1}>)\"}"

if [ ${CIRCLE_BRANCH} == "master" ] || [ ${CIRCLE_BRANCH} == "scheduled_e2e_testing" ] ; then
    curl -X POST -H 'Content-type: application/json' --data "${message}" ${SLACK_WEBHOOK_URL}
fi

