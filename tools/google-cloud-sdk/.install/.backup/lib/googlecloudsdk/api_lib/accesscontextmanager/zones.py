# -*- coding: utf-8 -*- #
# Copyright 2018 Google LLC. All Rights Reserved.
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
"""API library for access context manager zones."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.accesscontextmanager import util
from googlecloudsdk.api_lib.util import waiter
from googlecloudsdk.core import log
from googlecloudsdk.core import resources as core_resources


class Client(object):
  """High-level API client for access context access zones."""

  def __init__(self, client=None, messages=None, version='v1'):
    self.client = client or util.GetClient(version=version)
    self.messages = messages or self.client.MESSAGES_MODULE
    self.include_unrestricted_services = {
        'v1': False,
        'v1alpha': True,
        'v1beta': True
    }[version]

  def Get(self, zone_ref):
    return self.client.accessPolicies_servicePerimeters.Get(
        self.messages
        .AccesscontextmanagerAccessPoliciesServicePerimetersGetRequest(
            name=zone_ref.RelativeName()))

  def Patch(self,
            perimeter_ref,
            description=None,
            title=None,
            perimeter_type=None,
            resources=None,
            restricted_services=None,
            unrestricted_services=None,
            levels=None,
            ingress_allowed_services=None,
            vpc_allowed_services=None,
            bridge_allowed_services=None,
            enable_ingress_service_restriction=None,
            enable_vpc_service_restriction=None,
            enable_bridge_service_restriction=None):
    """Patch a service perimeter.

    Any non-None fields will be included in the update mask.

    Args:
      perimeter_ref: resources.Resource, reference to the perimeter to patch
      description: str, description of the zone or None if not updating
      title: str, title of the zone or None if not updating
      perimeter_type: PerimeterTypeValueValuesEnum type enum value for the level
        or None if not updating
      resources: list of str, the names of resources (for now, just
        'projects/...') in the zone or None if not updating.
      restricted_services: list of str, the names of services
        ('example.googleapis.com') that *are* restricted by the access zone or
        None if not updating.
      unrestricted_services: list of str, the names of services
        ('example.googleapis.com') that *are not* restricted by the access zone
        or None if not updating.
      levels: list of Resource, the access levels (in the same policy) that must
        be satisfied for calls into this zone or None if not updating.
      ingress_allowed_services: list of str, the names of services
        ('example.googleapis.com') that *are* allowed to use Access Levels to
        make a cross access zone boundary call, or None if not updating.
      vpc_allowed_services: list of str, the names of services
        ('example.googleapis.com') that *are* allowed to be made within the
        access zone, or None if not updating.
      bridge_allowed_services: list of str, the names of services
        ('example.googleapis.com') that *are* allowed to use the bridge access
        zone, or None if not updating.
      enable_ingress_service_restriction: bool, whether to restrict the set of
        APIs callable outside the access zone via Access Levels, or None if not
        updating.
      enable_vpc_service_restriction: bool, whether to restrict the set of APIs
        callable within the access zone, or None if not updating.
      enable_bridge_service_restriction: bool, whether to restrict the set of
        APIs callable using the bridge access zone, or None if not updating.

    Returns:
      AccessZone, the updated access zone
    """
    m = self.messages
    perimeter = m.ServicePerimeter()

    update_mask = []

    if description is not None:
      update_mask.append('description')
      perimeter.description = description
    if title is not None:
      update_mask.append('title')
      perimeter.title = title
    if perimeter_type is not None:
      update_mask.append('perimeterType')
      perimeter.perimeterType = perimeter_type
    status = m.ServicePerimeterConfig()
    status_mutated = False
    if resources is not None:
      update_mask.append('status.resources')
      status.resources = resources
      status_mutated = True
    if self.include_unrestricted_services and unrestricted_services is not None:
      update_mask.append('status.unrestrictedServices')
      status.unrestrictedServices = unrestricted_services
      status_mutated = True
    if restricted_services is not None:
      update_mask.append('status.restrictedServices')
      status.restrictedServices = restricted_services
      status_mutated = True
    if levels is not None:
      update_mask.append('status.accessLevels')
      status.accessLevels = [l.RelativeName() for l in levels]
      status_mutated = True

    def AddServiceRestrictionFields(allowed_services, enable_restriction,
                                    restriction_type):
      """Utility function for adding service restriction fields."""
      if allowed_services is None and enable_restriction is None:
        return False
      full_restriction_name = restriction_type + 'ServiceRestriction'

      # Set empty message if absent.
      if getattr(status, full_restriction_name) is None:
        restriction_message = getattr(
            m,
            restriction_type.capitalize() + 'ServiceRestriction')()
        setattr(status, full_restriction_name, restriction_message)

      if allowed_services is not None:
        update_mask.append('status.' + full_restriction_name +
                           '.allowedServices')
        restriction_message = getattr(status, full_restriction_name)
        restriction_message.allowedServices = allowed_services

      if enable_restriction is not None:
        update_mask.append('status.' + full_restriction_name +
                           '.enableRestriction')
        restriction_message = getattr(status, full_restriction_name)
        restriction_message.enableRestriction = enable_restriction

      return True

    status_mutated |= AddServiceRestrictionFields(
        allowed_services=ingress_allowed_services,
        enable_restriction=enable_ingress_service_restriction,
        restriction_type='ingress')
    status_mutated |= AddServiceRestrictionFields(
        allowed_services=vpc_allowed_services,
        enable_restriction=enable_vpc_service_restriction,
        restriction_type='vpc')
    status_mutated |= AddServiceRestrictionFields(
        allowed_services=bridge_allowed_services,
        enable_restriction=enable_bridge_service_restriction,
        restriction_type='bridge')

    if status_mutated:
      perimeter.status = status

    update_mask.sort()  # For ease-of-testing

    # No update mask implies no fields were actually edited, so this is a no-op.
    if not update_mask:
      log.warning(
          'The update specified results in an identical resource. Skipping request.'
      )
      return perimeter

    request_type = (
        m.AccesscontextmanagerAccessPoliciesServicePerimetersPatchRequest)
    request = request_type(
        servicePerimeter=perimeter,
        name=perimeter_ref.RelativeName(),
        updateMask=','.join(update_mask),
    )

    operation = self.client.accessPolicies_servicePerimeters.Patch(request)
    poller = util.OperationPoller(self.client.accessPolicies_servicePerimeters,
                                  self.client.operations, perimeter_ref)
    operation_ref = core_resources.REGISTRY.Parse(
        operation.name, collection='accesscontextmanager.operations')
    return waiter.WaitFor(
        poller, operation_ref,
        'Waiting for PATCH operation [{}]'.format(operation_ref.Name()))
