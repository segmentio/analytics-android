# -*- coding: utf-8 -*- #
# Copyright 2017 Google LLC. All Rights Reserved.
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

"""Cloud Pub/Sub topics update command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.pubsub import topics
from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.pubsub import resource_args
from googlecloudsdk.command_lib.util.args import labels_util
from googlecloudsdk.core import log


@base.ReleaseTracks(base.ReleaseTrack.ALPHA, base.ReleaseTrack.BETA)
class UpdateAlphaBeta(base.UpdateCommand):
  """Updates an existing Cloud Pub/Sub topic."""

  detailed_help = {
      'EXAMPLES': """\
          To update existing labels on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --update-labels=KEY1=VAL1,KEY2=VAL2

          To clear all labels on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --clear-labels

          To remove an existing label on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --remove-labels=KEY1,KEY2

          To update a Cloud Pub/Sub topic's message storage policy, run:

              $ {command} mytopic message-storage-policy-allowed-regions=some-cloud-region1,some-cloud-region2
          """
  }

  @staticmethod
  def Args(parser):
    """Registers flags for this command."""
    resource_args.AddTopicResourceArg(parser, 'to update.')
    labels_util.AddUpdateLabelsFlags(parser)

    msp_group = parser.add_group(
        mutex=True, help='Message storage policy options.')
    msp_group.add_argument(
        '--recompute-message-storage-policy',
        action='store_true',
        help='If given, Cloud Pub/Sub will recompute the regions where messages'
        ' can be stored at rest, based on your organization\'s "Resource '
        ' Location Restriction" policy.')
    msp_group.add_argument(
        '--message-storage-policy-allowed-regions',
        metavar='REGION',
        type=arg_parsers.ArgList(),
        help='A list of one or more Cloud regions where messages are allowed to'
        ' be stored at rest.')

  def Run(self, args):
    """This is what gets called when the user runs this command.

    Args:
      args: an argparse namespace. All the arguments that were provided to this
        command invocation.

    Returns:
      A serialized object (dict) describing the results of the operation.

    Raises:
      An HttpException if there was a problem calling the
      API topics.Patch command.
    """
    client = topics.TopicsClient()
    topic_ref = args.CONCEPTS.topic.Parse()

    labels_update = labels_util.ProcessUpdateArgsLazy(
        args, client.messages.Topic.LabelsValue,
        orig_labels_thunk=lambda: client.Get(topic_ref).labels)

    result = None
    try:
      result = client.Patch(
          topic_ref,
          labels_update.GetOrNone(),
          args.recompute_message_storage_policy,
          args.message_storage_policy_allowed_regions)
    except topics.NoFieldsSpecifiedError:
      operations = [
          'clear_labels', 'update_labels', 'remove_labels',
          'recompute_message_storage_policy',
          'message_storage_policy_allowed_regions'
      ]
      if not any(args.IsSpecified(arg) for arg in operations):
        raise
      log.status.Print('No update to perform.')
    else:
      log.UpdatedResource(topic_ref.RelativeName(), kind='topic')
    return result


@base.ReleaseTracks(base.ReleaseTrack.GA)
class UpdateGA(base.UpdateCommand):
  """Updates an existing Cloud Pub/Sub topic."""

  detailed_help = {
      'EXAMPLES': """\
          To update existing labels on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --update-labels=KEY1=VAL1,KEY2=VAL2

          To clear all labels on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --clear-labels

          To remove an existing label on a Cloud Pub/Sub topic, run:

              $ {command} mytopic --remove-labels=KEY1,KEY2
          """
  }

  @staticmethod
  def Args(parser):
    """Registers flags for this command."""
    resource_args.AddTopicResourceArg(parser, 'to update.')
    labels_util.AddUpdateLabelsFlags(parser)

  def Run(self, args):
    """This is what gets called when the user runs this command.

    Args:
      args: an argparse namespace. All the arguments that were provided to this
        command invocation.

    Returns:
      A serialized object (dict) describing the results of the operation.

    Raises:
      An HttpException if there was a problem calling the
      API topics.Patch command.
    """
    client = topics.TopicsClient()
    topic_ref = args.CONCEPTS.topic.Parse()

    labels_update = labels_util.ProcessUpdateArgsLazy(
        args, client.messages.Topic.LabelsValue,
        orig_labels_thunk=lambda: client.Get(topic_ref).labels)

    result = None
    try:
      result = client.Patch(
          topic_ref,
          labels_update.GetOrNone())
    except topics.NoFieldsSpecifiedError:
      operations = [
          'clear_labels', 'update_labels', 'remove_labels'
      ]
      if not any(args.IsSpecified(arg) for arg in operations):
        raise
      log.status.Print('No update to perform.')
    else:
      log.UpdatedResource(topic_ref.RelativeName(), kind='topic')
    return result
