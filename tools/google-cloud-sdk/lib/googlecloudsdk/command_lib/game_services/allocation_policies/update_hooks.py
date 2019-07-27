# -*- coding: utf-8 -*- #
# Copyright 2019 Google LLC. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Update hooks for Cloud Game Services Scaling Policy."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.command_lib.game_services import utils


def MatchClusters(ref, args, req):
  if args.match_clusters:
    req = utils.AddFieldToUpdateMask('cluster_selectors', req)
    if req.allocationPolicy is None:
      req.allocationPolicy = utils.GetApiMessage(ref).AllocationPolicy()
    req.allocationPolicy.clusterSelectors = utils.ParseMatchClusters(
        ref, args.match_clusters)
  return req
