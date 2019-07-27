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
"""Methods and constants for global access."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.container import api_adapter as container_api_adapter
from googlecloudsdk.api_lib.run import service
from googlecloudsdk.api_lib.runtime_config import util
from googlecloudsdk.api_lib.util import apis

from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources


CONTAINER_API_VERSION = 'v1beta1'

SERVERLESS_API_NAME = 'run'
SERVERLESS_API_VERSION = 'v1alpha1'


def GetServerlessClientInstance():
  return apis.GetClientInstance(SERVERLESS_API_NAME, SERVERLESS_API_VERSION)


def ListRegions(client):
  """Get the list of all available regions from control plane.

  Args:
    client: (base_api.BaseApiClient), instance of a client to use for the list
      request.

  Returns:
    A list of str, which are regions.
  """
  project_resource_relname = util.ProjectPath(
      properties.VALUES.core.project.Get(required=True))
  response = client.projects_locations.List(
      client.MESSAGES_MODULE.RunProjectsLocationsListRequest(
          name=project_resource_relname,
          pageSize=100))
  return [l.locationId for l in response.locations]


def ListServices(client, locations):
  """Get the global services for a OnePlatform project.

  Args:
    client: (base_api.BaseApiClient), instance of a client to use for the list
      request.
    locations: (str), The relative name of the locations resource
      with either an actual location name e.g.
      'projects/my-project/locations/us-central1)
      to query the specified location 'or a wildcard name, '-'
      (e.g. 'projects/my-project/locations/-') to query all locations.

  Returns:
    List of googlecloudsdk.api_lib.run import service.Service objects.
  """
  request = client.MESSAGES_MODULE.RunProjectsLocationsServicesListRequest(
      parent=locations
  )
  response = client.projects_locations_services.List(request)
  return [service.Service(
      item, client.MESSAGES_MODULE) for item in response.items]


def ListClusters(location=None):
  """Get all clusters with Cloud Run enabled.

  Args:
    location: str optional name of location to search for clusters in. Leaving
      this field blank will search in all locations.

  Returns:
    List of googlecloudsdk.third_party.apis.container.CONTAINER_API_VERSION
    import container_CONTAINER_API_VERSION_messages.Cluster objects
  """
  container_api = container_api_adapter.NewAPIAdapter(CONTAINER_API_VERSION)
  project = properties.VALUES.core.project.Get(required=True)

  response = container_api.ListClusters(project, location)
  if response.missingZones:
    log.warning('The following cluster locations did not respond: {}. '
                'List results may be incomplete.'.format(', '.join(
                    response.missingZones)))

  def _SortKey(cluster):
    return (cluster.zone, cluster.name)

  clusters = sorted(response.clusters, key=_SortKey)
  return [
      c for c in clusters if (c.addonsConfig.cloudRunConfig and
                              not c.addonsConfig.cloudRunConfig.disabled)
  ]


def ListVerifiedDomains(client):
  """Get all verified domains.

  Args:
    client: (base_api.BaseApiClient), instance of a client to use for the list
      request.

  Returns:
    List of client.MESSAGES_MODULE.AuthorizedDomain objects
  """

  # TODO(b/129705144) Hard-coding the region is temporary, the control
  # plane is working on the fix to not require a region at all.
  region = 'us-central1'
  project = properties.VALUES.core.project.Get(required=True)
  location = resources.REGISTRY.Parse(
      region,
      params={'projectsId': project},
      collection='run.projects.locations')
  msgs = client.MESSAGES_MODULE
  req = msgs.RunProjectsLocationsAuthorizeddomainsListRequest(
      parent=location.RelativeName())

  response = client.projects_locations_authorizeddomains.List(req)
  return response.domains
