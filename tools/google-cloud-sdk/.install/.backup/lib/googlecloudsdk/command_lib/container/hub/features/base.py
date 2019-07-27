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
"""Base classes for [enable|disable|describe] commands for Feature resource."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import exceptions as apitools_exceptions
from googlecloudsdk.api_lib.util import apis as core_apis
from googlecloudsdk.api_lib.util import waiter
from googlecloudsdk.calliope import base
from googlecloudsdk.core import exceptions
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources


class EnableCommand(base.CreateCommand):
  """Base class for the command that enables a Feature."""

  def RunCommand(self, args, **kwargs):
    try:
      project = properties.VALUES.core.project.GetOrFail()
      return CreateFeature(project, self.FEATURE_NAME, **kwargs)
    except apitools_exceptions.HttpUnauthorizedError as e:
      raise exceptions.Error(
          'You are not authorized to disable MultiClusterIngress Feature from project [{}]. '
          'Underlying error: {}'.format(project, e))
    except properties.RequiredPropertyError as e:
      raise exceptions.Error(
          'Failed to retrieve the project ID.')


class DisableCommand(base.DeleteCommand):
  """Base class for the command that disables a Feature."""

  def RunCommand(self, args):
    try:
      project_id = properties.VALUES.core.project.GetOrFail()
      name = 'projects/{0}/locations/global/features/{1}'.format(
          project_id, self.FEATURE_NAME)
      DeleteFeature(name)
    except apitools_exceptions.HttpUnauthorizedError as e:
      raise exceptions.Error(
          'You are not authorized to disable MultiClusterIngress Feature from project [{}]. '
          'Underlying error: {}'.format(project_id, e))
    except properties.RequiredPropertyError as e:
      raise exceptions.Error(
          'Failed to retrieve the project ID.')


class DescribeCommand(base.DescribeCommand):
  """Base class for the command that describes the status of a Feature."""

  def RunCommand(self, args):
    try:
      project_id = properties.VALUES.core.project.GetOrFail()
      name = 'projects/{0}/locations/global/features/{1}'.format(
          project_id, self.FEATURE_NAME)
      return GetFeature(name)
    except apitools_exceptions.HttpUnauthorizedError as e:
      raise exceptions.Error(
          'You are not authorized to see the status of MultiClusterIngress '
          'Feature from project [{}]. '
          'Underlying error: {}'.format(project_id, e))


def CreateMultiClusterIngressFeatureSpec(config_membership):
  client = core_apis.GetClientInstance('gkehub', 'v1alpha1')
  messages = client.MESSAGES_MODULE
  return messages.MultiClusterIngressFeatureSpec(
      configMembership=config_membership)


def CreateFeature(project, feature_id, **kwargs):
  """Creates a Feature resource in Hub.

  Args:
    project: the project in which to create the Feature
    feature_id: the value to use for the feature_id
    **kwargs: arguments for Feature object. For eg, multiclusterFeatureSpec

  Returns:
    the created Feature resource.

  Raises:
    - apitools.base.py.HttpError: if the request returns an HTTP error
    - exceptions raised by waiter.WaitFor()
  """
  client = core_apis.GetClientInstance('gkehub', 'v1alpha1')
  messages = client.MESSAGES_MODULE
  request = messages.GkehubProjectsLocationsGlobalFeaturesCreateRequest(
      feature=messages.Feature(**kwargs),
      parent='projects/{}/locations/global'.format(project),
      featureId=feature_id,
  )

  op = client.projects_locations_global_features.Create(request)
  op_resource = resources.REGISTRY.ParseRelativeName(
      op.name, collection='gkehub.projects.locations.operations')
  return waiter.WaitFor(
      waiter.CloudOperationPoller(client.projects_locations_global_features,
                                  client.projects_locations_operations),
      op_resource, 'Waiting for Feature to be created')


def GetFeature(name):
  """Gets a Feature resource from Hub.

  Args:
    name: the full resource name of the Feature to get, e.g.,
      projects/foo/locations/global/features/name.

  Returns:
    a Feature resource

  Raises:
    apitools.base.py.HttpError: if the request returns an HTTP error
  """

  client = core_apis.GetClientInstance('gkehub', 'v1alpha1')
  return client.projects_locations_global_features.Get(
      client.MESSAGES_MODULE.GkehubProjectsLocationsGlobalFeaturesGetRequest(
          name=name))


def DeleteFeature(name):
  """Deletes a Feature resource in Hub.

  Args:
    name: the full resource name of the Feature to delete, e.g.,
      projects/foo/locations/global/features/name.

  Raises:
    apitools.base.py.HttpError: if the request returns an HTTP error
  """

  client = core_apis.GetClientInstance('gkehub', 'v1alpha1')
  op = client.projects_locations_global_features.Delete(
      client.MESSAGES_MODULE
      .GkehubProjectsLocationsGlobalFeaturesDeleteRequest(name=name))
  op_resource = resources.REGISTRY.ParseRelativeName(
      op.name, collection='gkehub.projects.locations.operations')
  waiter.WaitFor(
      waiter.CloudOperationPollerNoResources(
          client.projects_locations_operations), op_resource,
      'Waiting for feature to be deleted')
