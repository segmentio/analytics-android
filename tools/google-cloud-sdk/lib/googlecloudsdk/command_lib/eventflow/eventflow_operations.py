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
"""Allows you to write surfaces in terms of logical Eventflow operations."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

import contextlib

from googlecloudsdk.api_lib.eventflow import metric_names
from googlecloudsdk.api_lib.eventflow import trigger
from googlecloudsdk.api_lib.util import apis
from googlecloudsdk.api_lib.util import apis_internal
from googlecloudsdk.core import metrics
from googlecloudsdk.core import resources


@contextlib.contextmanager
def Connect(conn_context):
  """Provide a EventflowOperations instance to use.

  If we're using the GKE Serverless Add-on, connect to the relevant cluster.
  Otherwise, connect to the right region of GSE.

  Arguments:
    conn_context: a context manager that yields a ConnectionInfo and manages a
      dynamic context that makes connecting to serverless possible.

  Yields:
    A EventflowOperations instance.
  """

  # The One Platform client is required for making requests against
  # endpoints that do not supported Kubernetes-style resource naming
  # conventions. The One Platform client must be initialized outside of a
  # connection context so that it does not pick up the api_endpoint_overrides
  # values from the connection context.
  # pylint: disable=protected-access
  op_client = apis_internal._GetClientInstance(
      conn_context.api_name,
      conn_context.api_version,
      ca_certs=conn_context.ca_certs)
  # pylint: enable=protected-access

  with conn_context as conn_info:
    # pylint: disable=protected-access
    client = apis_internal._GetClientInstance(
        conn_info.api_name,
        conn_info.api_version,
        # Only check response if not connecting to GKE
        check_response_func=apis.CheckResponseForApiEnablement
        if conn_context.supports_one_platform else None,
        ca_certs=conn_info.ca_certs)
    # pylint: enable=protected-access
    yield EventflowOperations(
        client,
        conn_info.api_name,
        conn_info.api_version,
        conn_info.region,
        op_client)


class EventflowOperations(object):
  """Client used by Eventflow to communicate with the actual API."""

  def __init__(self, client, api_name, api_version, region, op_client):
    """Inits EventflowOperations with given API clients.

    Args:
      client: The API client for interacting with Kubernetes Cloud Run APIs.
      api_name: str, The name of the Cloud Run API.
      api_version: str, The version of the Cloud Run API.
      region: str, The region of the control plane if operating against
        hosted Cloud Run, else None.
      op_client: The API client for interacting with One Platform APIs. Or
        None if interacting with Cloud Run on GKE.
    """
    self._client = client
    self._registry = resources.REGISTRY.Clone()
    self._registry.RegisterApiByName(api_name, api_version)
    self._temporary_build_template_registry = {}
    self._op_client = op_client
    self._region = region

  @property
  def _messages_module(self):
    return self._client.MESSAGES_MODULE

  def ListTriggers(self, namespace_ref):
    messages = self._messages_module
    request = messages.RunNamespacesTriggersListRequest(
        parent=namespace_ref.RelativeName())
    with metrics.RecordDuration(metric_names.LIST_TRIGGERS):
      response = self._client.namespaces_triggers.List(request)
    return [trigger.Trigger(item, messages) for item in response.items]
