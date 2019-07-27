# -*- coding: utf-8 -*- #
# Copyright 2019 Google Inc. All Rights Reserved.
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
"""Organization Security policy."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals


class OrgSecurityPolicy(object):
  """Abstracts Organization SecurityPolicy resource."""

  def __init__(self, ref=None, compute_client=None):
    self.ref = ref
    self._compute_client = compute_client

  @property
  def _client(self):
    return self._compute_client.apitools_client

  @property
  def _messages(self):
    return self._compute_client.messages

  def _MakeAddAssociationRequestTuple(self, association, security_policy_id,
                                      replace_existing_association):
    return (
        self._client.organizationSecurityPolicies, 'AddAssociation',
        self._messages.ComputeOrganizationSecurityPoliciesAddAssociationRequest(
            securityPolicyAssociation=association,
            securityPolicy=security_policy_id,
            replaceExistingAssociation=replace_existing_association))

  def _MakeDeleteAssociationRequestTuple(self, security_policy_id):
    return (self._client.organizationSecurityPolicies, 'RemoveAssociation',
            self._messages
            .ComputeOrganizationSecurityPoliciesRemoveAssociationRequest(
                name=self.ref.Name(), securityPolicy=security_policy_id))

  def _MakeListAssociationsRequestTuple(self, target_resource):
    return (self._client.organizationSecurityPolicies, 'ListAssociations',
            self._messages
            .ComputeOrganizationSecurityPoliciesListAssociationsRequest(
                targetResource=target_resource))

  def _MakeDeleteRequestTuple(self):
    return (self._client.organizationSecurityPolicies, 'Delete',
            self._messages.ComputeOrganizationSecurityPoliciesDeleteRequest(
                securityPolicy=self.ref.Name()))

  def _MakeUpdateRequestTuple(self, security_policy=None):
    return (self._client.organizationSecurityPolicies, 'Patch',
            self._messages.ComputeOrganizationSecurityPoliciesPatchRequest(
                securityPolicy=self.ref.Name(),
                securityPolicyResource=security_policy))

  def _MakeDescribeRequestTuple(self):
    return (self._client.organizationSecurityPolicies, 'Get',
            self._messages.ComputeOrganizationSecurityPoliciesGetRequest(
                securityPolicy=self.ref.Name()))

  def _MakeMoveRequestTuple(self, parent_id=None):
    return (self._client.organizationSecurityPolicies, 'Move',
            self._messages.ComputeOrganizationSecurityPoliciesMoveRequest(
                securityPolicy=self.ref.Name(), parentId=parent_id))

  def _MakeCopyRulesRequestTuple(self, source_security_policy=None):
    return (self._client.organizationSecurityPolicies, 'CopyRules',
            self._messages.ComputeOrganizationSecurityPoliciesCopyRulesRequest(
                securityPolicy=self.ref.Name(),
                sourceSecurityPolicy=source_security_policy))

  def _MakeListRequestTuple(self, parent_id):
    return (self._client.organizationSecurityPolicies, 'List',
            self._messages.ComputeOrganizationSecurityPoliciesListRequest(
                parentId=parent_id))

  def _MakeCreateRequestTuple(self, security_policy, parent_id):
    return (self._client.organizationSecurityPolicies, 'Insert',
            self._messages.ComputeOrganizationSecurityPoliciesInsertRequest(
                parentId=parent_id, securityPolicy=security_policy))

  def AddAssociation(self,
                     association=None,
                     security_policy_id=None,
                     replace_existing_association=False,
                     only_generate_request=False):
    """Sends request to add an association."""

    requests = [
        self._MakeAddAssociationRequestTuple(association, security_policy_id,
                                             replace_existing_association)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def DeleteAssociation(self,
                        security_policy_id=None,
                        only_generate_request=False):
    """Sends request to delete an association."""

    requests = [self._MakeDeleteAssociationRequestTuple(security_policy_id)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def ListAssociations(self, target_resource=None, only_generate_request=False):
    """Sends request to list all the associations."""

    requests = [self._MakeListAssociationsRequestTuple(target_resource)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Delete(self, only_generate_request=False):
    """Sends request to delete a security policy."""

    requests = [self._MakeDeleteRequestTuple()]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Update(self, only_generate_request=False, security_policy=None):
    """Sends request to update a security policy."""

    requests = [self._MakeUpdateRequestTuple(security_policy=security_policy)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Move(self, only_generate_request=False, parent_id=None):
    """Sends request to move the security policy to anther parent."""

    requests = [self._MakeMoveRequestTuple(parent_id=parent_id)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def CopyRules(self, only_generate_request=False, source_security_policy=None):
    """Sends request to copy all the rules from another security policy."""

    requests = [
        self._MakeCopyRulesRequestTuple(
            source_security_policy=source_security_policy)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Describe(self, only_generate_request=False):
    """Sends request to describe a security policy."""

    requests = [self._MakeDescribeRequestTuple()]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def List(self, parent_id=None, only_generate_request=False):
    """Sends request to list all the security policies."""

    requests = [self._MakeListRequestTuple(parent_id)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Create(self,
             security_policy=None,
             parent_id=None,
             only_generate_request=False):
    """Sends request to create a security policy."""

    requests = [self._MakeCreateRequestTuple(security_policy, parent_id)]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests


class OrgSecurityPolicyRule(object):
  """Abstracts Organization SecurityPolicy Rule."""

  def __init__(self, ref=None, compute_client=None):
    self.ref = ref
    self._compute_client = compute_client

  @property
  def _client(self):
    return self._compute_client.apitools_client

  @property
  def _messages(self):
    return self._compute_client.messages

  def _MakeCreateRequestTuple(self,
                              security_policy=None,
                              security_policy_rule=None):
    return (self._client.organizationSecurityPolicies, 'AddRule',
            self._messages.ComputeOrganizationSecurityPoliciesAddRuleRequest(
                securityPolicy=security_policy,
                securityPolicyRule=security_policy_rule))

  def _MakeDeleteRequestTuple(self, priority=None, security_policy=None):
    return (self._client.organizationSecurityPolicies, 'RemoveRule',
            self._messages.ComputeOrganizationSecurityPoliciesRemoveRuleRequest(
                securityPolicy=security_policy, priority=priority))

  def _MakeDescribeRequestTuple(self, priority=None, security_policy=None):
    return (self._client.organizationSecurityPolicies, 'GetRule',
            self._messages.ComputeOrganizationSecurityPoliciesGetRuleRequest(
                securityPolicy=security_policy, priority=priority))

  def _MakeUpdateRequestTuple(self,
                              priority=None,
                              security_policy=None,
                              security_policy_rule=None):
    return (self._client.organizationSecurityPolicies, 'PatchRule',
            self._messages.ComputeOrganizationSecurityPoliciesPatchRuleRequest(
                priority=priority,
                securityPolicy=security_policy,
                securityPolicyRule=security_policy_rule))

  def Create(self,
             security_policy=None,
             security_policy_rule=None,
             only_generate_request=False):
    """Sends request to create a security policy rule."""

    requests = [
        self._MakeCreateRequestTuple(
            security_policy=security_policy,
            security_policy_rule=security_policy_rule)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Delete(self,
             priority=None,
             security_policy_id=None,
             only_generate_request=False):
    """Sends request to delete a security policy rule."""

    requests = [
        self._MakeDeleteRequestTuple(
            priority=priority, security_policy=security_policy_id)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Describe(self,
               priority=None,
               security_policy_id=None,
               only_generate_request=False):
    """Sends request to describe a security policy rule."""

    requests = [
        self._MakeDescribeRequestTuple(
            priority=priority, security_policy=security_policy_id)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests

  def Update(self,
             priority=None,
             security_policy=None,
             security_policy_rule=None,
             only_generate_request=False):
    """Sends request to update a security policy rule."""

    requests = [
        self._MakeUpdateRequestTuple(
            priority=priority,
            security_policy=security_policy,
            security_policy_rule=security_policy_rule)
    ]
    if not only_generate_request:
      return self._compute_client.MakeRequests(requests)
    return requests
