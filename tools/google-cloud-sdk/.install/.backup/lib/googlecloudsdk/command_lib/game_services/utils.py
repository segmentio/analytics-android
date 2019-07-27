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
"""Utility functions for Cloud Game Services update commands."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.util import apis

GAME_SERVICES_API = 'gameservices'
API_VERSION = 'v1alpha'


def AddFieldToUpdateMask(field, patch_request):
  update_mask = patch_request.updateMask
  if update_mask:
    if update_mask.count(field) == 0:
      patch_request.updateMask = update_mask + ',' + field
  else:
    patch_request.updateMask = field
  return patch_request


def GetApiMessage(resource_ref):
  api_version = resource_ref.GetCollectionInfo().api_version
  return apis.GetMessagesModule(GAME_SERVICES_API, api_version)


def GetClient(api_version=API_VERSION):
  return apis.GetClientInstance(GAME_SERVICES_API, api_version)


def GetClientInstance(api_version=API_VERSION):
  return apis.GetClientClass(GAME_SERVICES_API, api_version)


def ParseMatchClusters(ref, match_clusters, messages=None):
  messages = messages or GetApiMessage(ref)

  cluster_selectors = []
  for clusters in match_clusters:
    cluster_labels = []
    for (key, val) in clusters.items():
      cluster_labels.append(ParseClusters(ref, key, val, messages=messages))
    cluster_selectors.append(ParseLabels(ref, cluster_labels))
  return cluster_selectors


def ParseClusters(ref, key, val, messages=None):
  messages = messages or GetApiMessage(ref)

  return messages.LabelSelector.LabelsValue.AdditionalProperty(
      key=key, value=val)


def ParseLabels(ref, cluster_labels, messages=None):
  messages = messages or GetApiMessage(ref)

  selectors = messages.LabelSelector.LabelsValue()
  selectors.additionalProperties = cluster_labels

  label_selector = messages.LabelSelector()
  label_selector.labels = selectors

  return label_selector
