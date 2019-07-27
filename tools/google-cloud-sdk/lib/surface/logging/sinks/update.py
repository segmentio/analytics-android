# -*- coding: utf-8 -*- #
# Copyright 2014 Google LLC. All Rights Reserved.
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

"""'logging sinks update' command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.logging import util
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions as calliope_exceptions
from googlecloudsdk.core import log
from googlecloudsdk.core.console import console_io


@base.ReleaseTracks(base.ReleaseTrack.GA, base.ReleaseTrack.BETA)
class Update(base.UpdateCommand):
  """Updates a sink.

  Changes the *[destination]* or *--log-filter* associated with a sink.
  The new destination must already exist and Stackdriver Logging must have
  permission to write to it.
  Log entries are exported to the new destination immediately.

  ## EXAMPLES

  To only update a sink filter, run:

    $ {command} my-sink --log-filter='severity>=ERROR'

  Detailed information about filters can be found at:
  [](https://cloud.google.com/logging/docs/view/advanced_filters)
  """

  @staticmethod
  def Args(parser):
    """Register flags for this command."""
    parser.add_argument(
        'sink_name', help='The name of the sink to update.')
    parser.add_argument(
        'destination', nargs='?',
        help=('A new destination for the sink. '
              'If omitted, the sink\'s existing destination is unchanged.'))
    parser.add_argument(
        '--log-filter', required=False,
        help=('A new filter expression for the sink. '
              'If omitted, the sink\'s existing filter (if any) is unchanged.'))
    util.AddNonProjectArgs(parser, 'Update a sink')

  def GetSink(self, parent, sink_ref):
    """Returns a sink specified by the arguments."""
    return util.GetClient().projects_sinks.Get(
        util.GetMessages().LoggingProjectsSinksGetRequest(
            sinkName=util.CreateResourceName(
                parent, 'sinks', sink_ref.sinksId)))

  def PatchSink(self, parent, sink_data, update_mask):
    """Patches a sink specified by the arguments."""
    messages = util.GetMessages()
    return util.GetClient().projects_sinks.Patch(
        messages.LoggingProjectsSinksPatchRequest(
            sinkName=util.CreateResourceName(parent, 'sinks',
                                             sink_data['name']),
            logSink=messages.LogSink(**sink_data),
            uniqueWriterIdentity=True,
            updateMask=','.join(update_mask)))

  def _Run(self, args, support_dlp=False):
    sink_ref = util.GetSinkReference(args.sink_name, args)

    sink_data = {'name': sink_ref.sinksId}
    update_mask = []
    if args.IsSpecified('destination'):
      sink_data['destination'] = args.destination
      update_mask.append('destination')
    if args.IsSpecified('log_filter'):
      sink_data['filter'] = args.log_filter
      update_mask.append('filter')

    parameter_names = ['[destination]', '--log-filter']
    dlp_options = {}
    if support_dlp:
      parameter_names.extend(
          ['--dlp-inspect-template', '--dlp-deidentify-template'])
      if args.IsSpecified('dlp_inspect_template'):
        dlp_options['inspectTemplateName'] = args.dlp_inspect_template
        update_mask.append('dlp_options.inspect_template_name')
      if args.IsSpecified('dlp_deidentify_template'):
        dlp_options['deidentifyTemplateName'] = args.dlp_deidentify_template
        update_mask.append('dlp_options.deidentify_template_name')
      if dlp_options:
        sink_data['dlpOptions'] = dlp_options

    if not update_mask:
      raise calliope_exceptions.MinimumArgumentException(
          parameter_names, 'Please specify at least one property to update')

    # Check for legacy configuration, and let users decide if they still want
    # to update the sink with new settings.
    sink = self.GetSink(util.GetParentFromArgs(args), sink_ref)
    if 'cloud-logs@' in sink.writerIdentity:
      console_io.PromptContinue(
          'This update will create a new writerIdentity (service account) for '
          'the sink. In order for the sink to continue working, grant that '
          'service account correct permission on the destination. The service '
          'account will be displayed after a successful update operation.',
          cancel_on_no=True, default=False)

    result = self.PatchSink(
        util.GetParentFromArgs(args), sink_data, update_mask)

    log.UpdatedResource(sink_ref)
    self._epilog_result_destination = result.destination
    self._epilog_writer_identity = result.writerIdentity
    self._epilog_is_dlp_sink = bool(dlp_options)
    return result

  def Run(self, args):
    """This is what gets called when the user runs this command.

    Args:
      args: an argparse namespace. All the arguments that were provided to this
        command invocation.

    Returns:
      The updated sink with its new destination.
    """
    return self._Run(args)

  def Epilog(self, unused_resources_were_displayed):
    util.PrintPermissionInstructions(self._epilog_result_destination,
                                     self._epilog_writer_identity,
                                     self._epilog_is_dlp_sink)


# pylint: disable=missing-docstring
@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class UpdateAlpha(Update):
  __doc__ = Update.__doc__

  @staticmethod
  def Args(parser):
    Update.Args(parser)
    dlp_group = parser.add_argument_group(
        help=('Settings for Cloud DLP enabled sinks. If any of these arguments '
              'are omitted they are unchanged.'))
    dlp_group.add_argument(
        '--dlp-inspect-template',
        required=False,
        help=('Relative path to a Cloud DLP inspection template resource. For '
              'example "projects/my-project/inspectTemplates/my-template" or '
              '"organizations/my-org/inspectTemplates/my-template".'))
    dlp_group.add_argument(
        '--dlp-deidentify-template',
        required=False,
        help=('Relative path to a Cloud DLP de-identification template '
              'resource. For example '
              '"projects/my-project/deidentifyTemplates/my-template" or '
              '"organizations/my-org/deidentifyTemplates/my-template".'))

  def Run(self, args):
    return self._Run(args, support_dlp=True)
