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
from googlecloudsdk.command_lib.util.args import labels_util
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources


def UpdateLabels(ref, args, req):
  """Update labels."""
  ref = GetResourceRef(args)
  labels_diff = labels_util.Diff.FromUpdateArgs(args)
  if labels_diff.MayHaveUpdates():
    req = utils.AddFieldToUpdateMask('labels', req)
    messages = GetMessagesForResource(ref)
    if req.scalingPolicy is None:
      req.scalingPolicy = utils.GetApiMessage(ref).ScalingPolicy()
    orig_resource = GetExistingResource(ref)
    new_labels = labels_diff.Apply(messages.LabelsValue,
                                   orig_resource.labels).GetOrNone()
    if new_labels:
      req.scalingPolicy.labels = new_labels
    else:
      req.scalingPolicy.labels = orig_resource.labels
  return req


def GetResourceRef(args):
  project = properties.VALUES.core.project.Get(required=True)
  location = args.location
  ref = resources.REGISTRY.Create(
      'gameservices.projects.locations.scalingPolicies', projectsId=project,
      locationsId=location, scalingPoliciesId=args.scaling_policy)
  return ref


def GetMessagesForResource(resource_ref):
  api_version = resource_ref.GetCollectionInfo().api_version
  get_request_message = GetRequestMessage(resource_ref)
  messages = utils.GetClient(
      api_version).projects_locations_scalingPolicies.Get(get_request_message)
  return messages


def GetExistingResource(resource_ref):
  api_version = resource_ref.GetCollectionInfo().api_version
  get_request_message = GetRequestMessage(resource_ref)
  orig_resource = utils.GetClient(
      api_version).projects_locations_scalingPolicies.Get(get_request_message)
  return orig_resource


def GetRequestMessage(resource_ref):
  return utils.GetApiMessage(
      resource_ref).GameservicesProjectsLocationsScalingPoliciesGetRequest(
          name=resource_ref.RelativeName())


def MatchClusters(ref, args, req):
  if args.match_clusters:
    req = utils.AddFieldToUpdateMask('cluster_selectors.labels', req)
    if req.scalingPolicy is None:
      req.scalingPolicy = utils.GetApiMessage(ref).ScalingPolicy()
    req.scalingPolicy.clusterSelectors = utils.ParseMatchClusters(
        ref, args.match_clusters)
  return req
