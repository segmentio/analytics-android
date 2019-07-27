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
"""Update an existing secret."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.secrets import api as secrets_api
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.calliope import parser_errors
from googlecloudsdk.command_lib.secrets import args as secrets_args
from googlecloudsdk.command_lib.secrets import log as secrets_log
from googlecloudsdk.command_lib.secrets import util as secrets_util
from googlecloudsdk.command_lib.util.args import labels_util


class Update(base.UpdateCommand):
  r"""Update a secret's metadata and data.

  Update a secret's metadata (e.g. locations, labels) and data. This command
  will return an error if given a secret that does not exist. To upsert the
  creation of a secret, use the `--create-if-missing` flag.

  ## EXAMPLES

  Update location of a secret named 'my-secret'.

    $ {command} my-secret --locations=us-central1

  Update the value data of the secret named 'my-secret', creating the secret if
  does not already exist:

    $ {command} my-secret --data-file=/tmp/secret --create-if-missing
  """

  CREATE_IF_MISSING_MESSAGE = (
      'The secret [{secret}] cannot be updated because it does not exist. To '
      'create the secret if it does not exist, specify the --create-if-missing '
      'flag on the update command.')

  NO_CHANGES_MESSAGE = (
      'There are no changes to the secret [{secret}] for update.')

  @staticmethod
  def Args(parser):
    secrets_args.AddSecret(
        parser, purpose='to update', positional=True, required=True)
    secrets_args.AddDataFile(parser)
    secrets_args.AddCreateIfMissing(parser, resource='secret')
    secrets_args.AddLocations(parser, resource='secret')
    labels_util.AddUpdateLabelsFlags(parser)

  def _RunCreate(self, args):
    messages = secrets_api.GetMessages()
    secret_ref = args.CONCEPTS.secret.Parse()
    data = secrets_util.ReadFileOrStdin(args.data_file)

    locations = args.locations
    if not locations:
      raise parser_errors.RequiredError(argument='--locations')

    labels = labels_util.Diff.FromUpdateArgs(args).Apply(
        messages.Secret.LabelsValue).GetOrNone()

    secret = secrets_api.Secrets().Create(
        secret_ref=secret_ref, labels=labels, locations=locations)
    secrets_log.Secrets().Created(secret_ref)

    if not data:
      return secret

    version = secrets_api.Secrets().SetData(secret_ref, data)
    version_ref = secrets_args.ParseVersionRef(version.name)
    secrets_log.Versions().Created(version_ref)
    return version

  def _RunUpdate(self, original, args):
    messages = secrets_api.GetMessages()
    secret_ref = args.CONCEPTS.secret.Parse()
    data = secrets_util.ReadFileOrStdin(args.data_file)

    # Collect the list of update masks
    update_mask = []

    labels_diff = labels_util.Diff.FromUpdateArgs(args)
    if labels_diff.MayHaveUpdates():
      update_mask.append('labels')

    locations = args.locations
    if locations:
      update_mask.append('policy.replicaLocations')

    update_mask.sort()

    # Validations
    if not update_mask and not data:
      raise exceptions.ToolException(
          self.NO_CHANGES_MESSAGE.format(secret=secret_ref.Name()))

    if update_mask:
      labels = labels_diff.Apply(messages.Secret.LabelsValue,
                                 original.labels).GetOrNone()

      secret = secrets_api.Secrets().Update(
          secret_ref=secret_ref,
          locations=args.locations,
          labels=labels,
          update_mask=update_mask)
      secrets_log.Secrets().Updated(secret_ref)

    if not data:
      return secret

    version = secrets_api.Secrets().SetData(secret_ref, data)
    version_ref = secrets_args.ParseVersionRef(version.name)
    secrets_log.Versions().Created(version_ref)
    return version

  def Run(self, args):
    secret_ref = args.CONCEPTS.secret.Parse()

    # Attempt to get the secret
    secret = secrets_api.Secrets().GetOrNone(secret_ref)

    # Secret does not exist
    if secret is None:
      if args.create_if_missing:
        return self._RunCreate(args)
      raise exceptions.ToolException(
          self.CREATE_IF_MISSING_MESSAGE.format(secret=secret_ref.Name()))

    # The secret exists, update it
    return self._RunUpdate(secret, args)
