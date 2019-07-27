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
"""The unregister-cluster command for removing clusters from the Hub."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import exceptions as apitools_exceptions
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.container.hub import util as hub_util
from googlecloudsdk.command_lib.util.apis import arg_utils
from googlecloudsdk.core import exceptions
from googlecloudsdk.core import log


class UnregisterCluster(base.DeleteCommand):
  r"""Unregisters a cluster from Google Cloud Platform.

  This command unregisters a cluster referenced from a kubeconfig file from
  Google Cloud Platform. It also removes the Connect agent installation from the
  Cluster.

  ## EXAMPLES

  Unregister a cluster referenced from the default kubeconfig file:

      $ {command} --context=my-cluster-context

  Unregister a cluster referenced from a specific kubeconfig file:

      $ {command} \
          --kubeconfig=/home/user/custom_kubeconfig \
          --context=my-cluster-context
  """

  @classmethod
  def Args(cls, parser):
    hub_util.AddCommonArgs(parser)

  def Run(self, args):
    project = arg_utils.GetFromNamespace(args, '--project', use_defaults=True)
    kube_client = hub_util.KubernetesClient(args)
    registered_project = hub_util.GetMembershipCROwnerID(kube_client)
    authorized_projects = hub_util.UserAccessibleProjectIDSet()
    if registered_project:
      if registered_project not in authorized_projects:
        raise exceptions.Error(
            'The cluster is already registered to [{}], which you are not '
            'authorized to access.'.format(registered_project))
      elif registered_project != project:
        raise exceptions.Error(
            'This cluster is registered to another project [{}]. '
            'Please unregister this cluster from the correct project:\n\n'
            '  gcloud {}container hub unregister-cluster --project {} --context {}'
            .format(registered_project,
                    hub_util.ReleaseTrackCommandPrefix(self.ReleaseTrack()),
                    registered_project, args.context))

    if project not in authorized_projects:
      raise exceptions.Error(
          'The project you are attempting to register with [{}] either '
          'doesn\'t exist or you are not authorized to access it.'.format(
              project))

    uuid = hub_util.GetClusterUUID(kube_client)
    try:
      registered_membership_project = hub_util.ProjectForClusterUUID(
          uuid, [project, registered_project])
    except apitools_exceptions.HttpNotFoundError as e:
      raise exceptions.Error(
          'Could not access Memberships API. Is your project whitelisted for '
          'API access? Underlying error: {}'.format(e))

    if registered_membership_project and project != registered_membership_project:
      raise exceptions.Error(
          'This cluster is registered to another project [{}]. '
          'Please unregister this cluster from the appropriate project:\n\n'
          '  gcloud {}container hub unregister-cluster --project {} --context {}'
          .format(registered_membership_project,
                  hub_util.ReleaseTrackCommandPrefix(self.ReleaseTrack()),
                  registered_membership_project, args.context))

    if not registered_membership_project:
      log.status.Print(
          'Membership for [{}] was not found. It may already have been '
          'deleted, or it may never have existed. You can safely run the '
          '`register-cluster` command again for this cluster.'.format(
              args.context))
      # There is no Membership for this cluster, but there is a Membership CR.
      # We can safely remove the Membership CR, so users can register to another
      # hub without issue.
      if registered_project:
        hub_util.DeleteMembershipResources(kube_client)
      return
    hub_util.DeleteConnectNamespace(args)

    try:
      name = 'projects/{}/locations/global/memberships/{}'.format(project, uuid)
      hub_util.DeleteMembership(name)
      hub_util.DeleteMembershipResources(kube_client)
    except apitools_exceptions.HttpUnauthorizedError as e:
      raise exceptions.Error(
          'You are not authorized to unregister clusters from project [{}]. '
          'Underlying error: {}'.format(project, e))
