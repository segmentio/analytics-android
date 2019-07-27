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
"""Provides common arguments for the Run command surface."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

import os
import re

from googlecloudsdk.api_lib.container import kubeconfig
from googlecloudsdk.api_lib.run import global_methods
from googlecloudsdk.api_lib.services import enable_api
from googlecloudsdk.calliope import actions
from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.calliope import exceptions as calliope_exceptions
from googlecloudsdk.command_lib.functions.deploy import env_vars_util
from googlecloudsdk.command_lib.run import config_changes
from googlecloudsdk.command_lib.run import exceptions as serverless_exceptions
from googlecloudsdk.command_lib.run import pretty_print
from googlecloudsdk.command_lib.util.args import labels_util
from googlecloudsdk.command_lib.util.args import map_util
from googlecloudsdk.command_lib.util.args import repeated
from googlecloudsdk.core import exceptions
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources
from googlecloudsdk.core.console import console_io
from googlecloudsdk.core.util import files
from googlecloudsdk.core.util import times

_VISIBILITY_MODES = {
    'internal': 'Visible only within the cluster.',
    'external': 'Visible from outside the cluster.',
}

_PLATFORMS = {
    'managed': 'Fully managed version of Cloud Run. Use with the `--region` '
               'flag or set the [run/region] property to specify a Cloud Run '
               'region.',
    'gke': 'Cloud Run on Google Kubernetes Engine. Use with the `--cluster` '
           'and `--cluster-location` flags or set the [run/cluster] and '
           '[run/cluster_location] properties to specify a cluster in a given '
           'zone.'
}

_PLATFORMS_ALPHA = {
    'kubernetes': 'Use a Knative-compatible kubernetes cluster. Use with the '
                  '`--kubeconfig` and `--context` flags to specify a '
                  'kubeconfig file and the context for connecting.'
}
_PLATFORMS_ALPHA.update(_PLATFORMS)

_PLATFORM_SHORT_DESCRIPTIONS = {
    'managed': 'the managed version of Cloud Run',
    'gke': 'Cloud Run on GKE',
    'kubernetes': 'a Kubernetes cluster'
}

_DEFAULT_KUBECONFIG_PATH = '~/.kube/config'


class ArgumentError(exceptions.Error):
  pass


class KubeconfigError(exceptions.Error):
  pass


def _AddSourceArg(parser):
  """Add a source resource arg."""
  parser.add_argument(
      '--source',
      # TODO(b/110538411): re-expose source arg when it's time.
      hidden=True,
      help="""\
      The app source. Defaults to the working directory. May be a GCS bucket,
      Google source code repository, or directory on the local filesystem.
      """)


def _AddImageArg(parser):
  """Add an image resource arg."""
  parser.add_argument(
      '--image',
      help='Name of the container image to deploy (e.g. '
      '`gcr.io/cloudrun/hello:latest`).')


_ARG_GROUP_HELP_TEXT = ('Only applicable if connecting to {platform_desc}. '
                        'Specify {platform} to use:')


def _GetOrAddArgGroup(parser, help_text):
  """Create a new arg group or return existing group with given help text."""
  for arg in parser.arguments:
    if arg.is_group and arg.help == help_text:
      return arg
  return parser.add_argument_group(help_text)


def GetManagedArgGroup(parser):
  """Get an arg group for managed CR-only flags."""
  return _GetOrAddArgGroup(
      parser,
      _ARG_GROUP_HELP_TEXT.format(
          platform='\'--platform=managed\'',
          platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))


def GetGkeArgGroup(parser):
  """Get an arg group for CRoGKE-only flags."""
  return _GetOrAddArgGroup(
      parser,
      _ARG_GROUP_HELP_TEXT.format(
          platform='\'--platform=gke\'',
          platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))


def GetKubernetesArgGroup(parser):
  """Get an arg group for --platform=kubernetes only flags."""
  return _GetOrAddArgGroup(
      parser,
      _ARG_GROUP_HELP_TEXT.format(
          platform='\'--platform=kubernetes\'',
          platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['kubernetes']))


def GetClusterArgGroup(parser):
  """Get an arg group for any generic cluster flags."""
  return _GetOrAddArgGroup(
      parser,
      _ARG_GROUP_HELP_TEXT.format(
          platform='\'--platform=gke\' or \'--platform=kubernetes\'',
          platform_desc='{} or {}'.format(
              _PLATFORM_SHORT_DESCRIPTIONS['gke'],
              _PLATFORM_SHORT_DESCRIPTIONS['kubernetes'])))


def AddAllowUnauthenticatedFlag(parser):
  """Add the --allow-unauthenticated flag."""
  parser.add_argument(
      '--allow-unauthenticated',
      action=arg_parsers.StoreTrueFalseAction,
      help='Whether to enable allowing unauthenticated access to the service.')


def AddAsyncFlag(parser):
  """Add an async flag."""
  parser.add_argument(
      '--async',
      default=False,
      action='store_true',
      help='True to deploy asynchronously.')


def AddEndpointVisibilityEnum(parser):
  """Add the --connectivity=[external|internal] flag."""
  parser.add_argument(
      '--connectivity',
      choices=_VISIBILITY_MODES,
      help=('Defaults to \'external\'. If \'external\', the service can be '
            'invoked through the internet, in addition to through the cluster '
            'network.'))


def AddServiceFlag(parser):
  """Add a service resource flag."""
  parser.add_argument(
      '--service',
      required=False,
      help='Limit matched revisions to the given service.')


def AddSourceRefFlags(parser):
  """Add the image and source args."""
  _AddImageArg(parser)


def AddRegionArg(parser):
  """Add a region arg."""
  parser.add_argument(
      '--region',
      help='Region in which the resource can be found. '
      'Alternatively, set the property [run/region].')


# TODO(b/118339293): When global list endpoint ready, stop hardcoding regions.
def AddRegionArgWithDefault(parser):
  """Add a region arg which defaults to us-central1.

  This is used by commands which list global resources.

  Args:
    parser: ArgumentParser, The calliope argparse parser.
  """
  parser.add_argument(
      '--region',
      default='us-central1',
      help='Region in which to list the resources.')


def AddFunctionArg(parser):
  """Add a function resource arg."""
  parser.add_argument(
      '--function',
      hidden=True,
      help="""\
      Specifies that the deployed object is a function. If a value is
      provided, that value is used as the entrypoint.
      """)


def AddCloudSQLFlags(parser):
  """Add flags for setting CloudSQL stuff."""
  repeated.AddPrimitiveArgs(
      parser,
      'Service',
      'cloudsql-instances',
      'Cloud SQL instances',
      auto_group_help=False,
      additional_help="""\
      These flags modify the Cloud SQL instances this Service connects to.
      You can specify a name of a Cloud SQL instance if it's in the same
      project and region as your Cloud Run service; otherwise specify
      <project>:<region>:<instance> for the instance.""")


def AddMapFlagsNoFile(parser,
                      flag_name,
                      group_help='',
                      long_name=None,
                      key_type=None,
                      value_type=None):
  """Add flags like map_util.AddUpdateMapFlags but without the file one.

  Args:
    parser: The argument parser
    flag_name: The name for the property to be used in flag names
    group_help: Help text for the group of flags
    long_name: The name for the property to be used in help text
    key_type: A function to apply to map keys.
    value_type: A function to apply to map values.
  """
  if not long_name:
    long_name = flag_name

  group = parser.add_mutually_exclusive_group(group_help)
  update_remove_group = group.add_argument_group(
      help=('Only --update-{0} and --remove-{0} can be used together. If both '
            'are specified, --remove-{0} will be applied first.'
           ).format(flag_name))
  map_util.AddMapUpdateFlag(
      update_remove_group,
      flag_name,
      long_name,
      key_type=key_type,
      value_type=value_type)
  map_util.AddMapRemoveFlag(
      update_remove_group, flag_name, long_name, key_type=key_type)
  map_util.AddMapClearFlag(group, flag_name, long_name)
  map_util.AddMapSetFlag(
      group, flag_name, long_name, key_type=key_type, value_type=value_type)


def AddMutexEnvVarsFlags(parser):
  """Add flags for creating updating and deleting env vars."""
  # TODO(b/119837621): Use env_vars_util.AddUpdateEnvVarsFlags when
  # `gcloud run` supports an env var file.
  AddMapFlagsNoFile(
      parser,
      flag_name='env-vars',
      long_name='environment variables',
      key_type=env_vars_util.EnvVarKeyType,
      value_type=env_vars_util.EnvVarValueType)


def AddMemoryFlag(parser):
  parser.add_argument('--memory', help='Set a memory limit. Ex: 1Gi, 512Mi.')


def AddCpuFlag(parser):
  parser.add_argument(
      '--cpu',
      help='Set a CPU limit in Kubernetes cpu units. '
      'Ex: .5, 500m, 2.')


def AddConcurrencyFlag(parser):
  parser.add_argument(
      '--concurrency',
      help='Set the number of concurrent requests allowed per '
      'instance. A concurrency of 0 or unspecified indicates '
      'any number of concurrent requests are allowed. To unset '
      'this field, provide the special value `default`.')


def AddTimeoutFlag(parser):
  parser.add_argument(
      '--timeout',
      help='Set the maximum request execution time (timeout). It is specified '
      'as a duration; for example, "10m5s" is ten minutes, and five seconds. '
      'If you don\'t specify a unit, seconds is assumed. For example, "10" is '
      '10 seconds.')


def AddServiceAccountFlag(parser):
  parser.add_argument(
      '--service-account',
      help='Email address of the IAM service account associated with the '
      'revision of the service. The service account represents the identity of '
      'the running revision, and determines what permissions the revision has. '
      'If not provided, the revision will use the project\'s default service '
      'account.')


def AddPlatformArg(parser):
  """Add a platform arg."""
  parser.add_argument(
      '--platform',
      choices=_PLATFORMS,
      action=actions.StoreProperty(properties.VALUES.run.platform),
      help='Target platform for running commands. '
      'Alternatively, set the property [run/platform]. '
      'If not specified, the user will be prompted to choose a platform.')


def AddAlphaPlatformArg(parser):
  """Add a platform arg with alpha choices."""
  parser.add_argument(
      '--platform',
      choices=_PLATFORMS_ALPHA,
      action=actions.StoreProperty(properties.VALUES.run.platform),
      help='Target platform for running commands. '
      'Alternatively, set the property [run/platform]. '
      'If not specified, the user will be prompted to choose a platform.')


def AddKubeconfigFlags(parser):
  parser.add_argument(
      '--kubeconfig',
      help='The absolute path to your kubectl config file. If not specified, '
      'the colon- or semicolon-delimited list of paths specified by '
      '$KUBECONFIG will be used. If $KUBECONFIG is unset, this defaults to '
      '`{}`.'.format(_DEFAULT_KUBECONFIG_PATH))
  parser.add_argument(
      '--context',
      help='The name of the context in your kubectl config file to use for '
      'connecting.')


def AddRevisionSuffixArg(parser):
  parser.add_argument(
      '--revision-suffix',
      hidden=True,
      help='Specify the suffix of the revision name. Revision names always '
      'start with the service name automatically. For example, specifying '
      '[--revision-suffix=v1] for a service named \'helloworld\', '
      'would lead to a revision named \'helloworld-v1\'.')


def AddVpcConnectorArg(parser):
  parser.add_argument(
      '--vpc-connector', help='Set a VPC connector for this Service.')
  parser.add_argument(
      '--clear-vpc-connector',
      action='store_true',
      help='Remove the VPC connector for this Service.')


def AddSecretsFlags(parser):
  """Add flags for creating, updating, and deleting secrets."""
  AddMapFlagsNoFile(
      parser,
      group_help='Specify where to mount which secrets. '
      'Mount paths map to a secret name. '
      'Optionally, add an additional parameter to specify a '
      'volume name for the secret. For example, '
      '\'--update-secrets=/my/path=mysecret:secretvol\' will '
      'create a volume named \'secretvol\' with a secret '
      'named \'mysecret\' and mount that volume at \'/my/path\'. '
      'If a volume name is not provided, the secret name '
      'will be used.',
      flag_name='secrets',
      long_name='secret mount paths')


def AddConfigMapsFlags(parser):
  """Add flags for creating, updating, and deleting config maps."""
  AddMapFlagsNoFile(
      parser,
      group_help='Specify where to mount which config maps. '
      'Mount paths map to a config map name. '
      'Optionally, add an additional parameter to specify a '
      'volume name for the config map. For example, '
      '\'--update-config-maps=/my/path=myconfig:configvol\' will '
      'create a volume named \'configvol\' with a config map '
      'named \'myconfig\' and mount that volume at \'/my/path\'. '
      'If a volume name is not provided, the config map name '
      'will be used.',
      flag_name='config-maps',
      long_name='config map mount paths')


def _HasChanges(args, flags):
  """True iff any of the passed flags are set."""
  # hasattr check is to allow the same code to work for release tracks that
  # don't have the args at all yet.
  return any(hasattr(args, flag) and args.IsSpecified(flag) for flag in flags)


def _HasEnvChanges(args):
  """True iff any of the env var flags are set."""
  env_flags = [
      'update_env_vars', 'set_env_vars', 'remove_env_vars', 'clear_env_vars'
  ]
  return _HasChanges(args, env_flags)


def _HasCloudSQLChanges(args):
  """True iff any of the cloudsql flags are set."""
  instances_flags = [
      'add_cloudsql_instances', 'set_cloudsql_instances',
      'remove_cloudsql_instances', 'clear_cloudsql_instances'
  ]
  return _HasChanges(args, instances_flags)


def _HasLabelChanges(args):
  """True iff any of the label flags are set."""
  label_flags = ['labels', 'update_labels', 'clear_labels', 'remove_labels']
  return _HasChanges(args, label_flags)


def _HasSecretsChanges(args):
  """True iff any of the secret flags are set."""
  secret_flags = [
      'update_secrets', 'set_secrets', 'remove_secrets', 'clear_secrets'
  ]
  return _HasChanges(args, secret_flags)


def _HasConfigMapsChanges(args):
  """True iff any of the config maps flags are set."""
  config_maps_flags = [
      'update_config_maps', 'set_config_maps', 'remove_config_maps',
      'clear_config_maps'
  ]
  return _HasChanges(args, config_maps_flags)


def _GetEnvChanges(args):
  """Return config_changes.EnvVarChanges for given args."""
  kwargs = {}

  update = args.update_env_vars or args.set_env_vars
  if update:
    kwargs['env_vars_to_update'] = update

  remove = args.remove_env_vars
  if remove:
    kwargs['env_vars_to_remove'] = remove

  if args.set_env_vars or args.clear_env_vars:
    kwargs['clear_others'] = True

  return config_changes.EnvVarChanges(**kwargs)


def _GetSecretsChanges(args):
  """Return config_changes.VolumeChanges for given args."""
  kwargs = {}

  update = args.update_secrets or args.set_secrets
  if update:
    kwargs['mounts_to_update'] = update

  remove = args.remove_secrets
  if remove:
    kwargs['mounts_to_remove'] = remove

  if args.set_secrets or args.clear_secrets:
    kwargs['clear_others'] = True

  return config_changes.VolumeChanges('secrets', **kwargs)


def _GetConfigMapsChanges(args):
  """Return config_changes.VolumeChanges for given args."""
  kwargs = {}

  update = args.update_config_maps or args.set_config_maps
  if update:
    kwargs['mounts_to_update'] = update

  remove = args.remove_config_maps
  if remove:
    kwargs['mounts_to_remove'] = remove

  if args.set_config_maps or args.clear_config_maps:
    kwargs['clear_others'] = True

  return config_changes.VolumeChanges('config_maps', **kwargs)


def PromptToEnableApi(service_name):
  """Prompts to enable the API and throws if the answer is no.

  Args:
    service_name: str, The service token of the API to prompt for.
  """
  if not properties.VALUES.core.should_prompt_to_enable_api.GetBool():
    return

  project = properties.VALUES.core.project.Get(required=True)
  # Don't prompt to enable an already enabled API
  if not enable_api.IsServiceEnabled(project, service_name):
    if console_io.PromptContinue(
        default=False,
        cancel_on_no=True,
        prompt_string=('API [{}] not enabled on project [{}]. '
                       'Would you like to enable and retry (this will take a '
                       'few minutes)?').format(service_name, project)):
      enable_api.EnableService(project, service_name)


_CLOUD_SQL_API_SERVICE_TOKEN = 'sql-component.googleapis.com'
_CLOUD_SQL_ADMIN_API_SERVICE_TOKEN = 'sqladmin.googleapis.com'


def _CheckCloudSQLApiEnablement():
  if not properties.VALUES.core.should_prompt_to_enable_api.GetBool():
    return
  PromptToEnableApi(_CLOUD_SQL_API_SERVICE_TOKEN)
  PromptToEnableApi(_CLOUD_SQL_ADMIN_API_SERVICE_TOKEN)


def GetConfigurationChanges(args):
  """Returns a list of changes to Configuration, based on the flags set."""
  changes = []
  if _HasEnvChanges(args):
    changes.append(_GetEnvChanges(args))

  if _HasCloudSQLChanges(args):
    region = GetRegion(args)
    project = (
        getattr(args, 'project', None) or
        properties.VALUES.core.project.Get(required=True))
    _CheckCloudSQLApiEnablement()
    changes.append(config_changes.CloudSQLChanges(project, region, args))

  if _HasSecretsChanges(args):
    changes.append(_GetSecretsChanges(args))

  if _HasConfigMapsChanges(args):
    changes.append(_GetConfigMapsChanges(args))

  if 'cpu' in args and args.cpu:
    changes.append(config_changes.ResourceChanges(cpu=args.cpu))
  if 'memory' in args and args.memory:
    changes.append(config_changes.ResourceChanges(memory=args.memory))
  if 'concurrency' in args and args.concurrency:
    try:
      c = int(args.concurrency)
    except ValueError:
      c = args.concurrency
      if c != 'default':
        log.warning('Specifying concurrency as Single or Multi is deprecated; '
                    'an integer is preferred.')
    changes.append(config_changes.ConcurrencyChanges(concurrency=c))
  if 'timeout' in args and args.timeout:
    try:
      # A bare number is interpreted as seconds.
      timeout_secs = int(args.timeout)
    except ValueError:
      timeout_duration = times.ParseDuration(args.timeout)
      timeout_secs = int(timeout_duration.total_seconds)
    if timeout_secs <= 0:
      raise ArgumentError(
          'The --timeout argument must be a positive time duration.')
    changes.append(config_changes.TimeoutChanges(timeout=timeout_secs))
  if 'service_account' in args and args.service_account:
    changes.append(
        config_changes.ServiceAccountChanges(
            service_account=args.service_account))
  if _HasLabelChanges(args):
    additions = (
        args.labels
        if _FlagIsExplicitlySet(args, 'labels') else args.update_labels)
    diff = labels_util.Diff(
        additions=additions,
        subtractions=args.remove_labels,
        clear=args.clear_labels)
    if diff.MayHaveUpdates():
      changes.append(config_changes.LabelChanges(diff))
  if 'revision_suffix' in args and args.revision_suffix:
    changes.append(config_changes.RevisionNameChanges(args.revision_suffix))
  if 'vpc_connector' in args and args.vpc_connector:
    changes.append(config_changes.VpcConnectorChange(args.vpc_connector))
  if 'clear_vpc_connector' in args and args.clear_vpc_connector:
    changes.append(config_changes.ClearVpcConnectorChange())

  return changes


def GetService(args):
  """Get and validate the service resource from the args."""
  service_ref = args.CONCEPTS.service.Parse()
  # Valid service names comprise only alphanumeric characters and dashes. Must
  # not begin or end with a dash, and must not contain more than 63 characters.
  # Must be lowercase.
  service_re = re.compile(r'(?=^[a-z0-9-]{1,63}$)(?!^\-.*)(?!.*\-$)')
  if service_re.match(service_ref.servicesId):
    return service_ref
  raise ArgumentError(
      'Invalid service name [{}]. Service name must use only lowercase '
      'alphanumeric characters and dashes. Cannot begin or end with a dash, '
      'and cannot be longer than 63 characters.'.format(service_ref.servicesId))


def GetClusterRef(cluster):
  project = properties.VALUES.core.project.Get(required=True)
  return resources.REGISTRY.Parse(
      cluster.name,
      params={
          'projectId': project,
          'zone': cluster.zone
      },
      collection='container.projects.zones.clusters')


def PromptForRegion():
  """Prompt for region from list of available regions.

  This method is referenced by the declaritive iam commands as a fallthrough
  for getting the region.

  Returns:
    The region specified by the user, str
  """
  if console_io.CanPrompt():
    client = global_methods.GetServerlessClientInstance()
    all_regions = global_methods.ListRegions(client)
    idx = console_io.PromptChoice(
        all_regions, message='Please specify a region:\n', cancel_option=True)
    region = all_regions[idx]
    log.status.Print('To make this the default region, run '
                     '`gcloud config set run/region {}`.\n'.format(region))
    return region


def GetRegion(args, prompt=False):
  """Prompt for region if not provided.

  Region is decided in the following order:
  - region argument;
  - run/region gcloud config;
  - compute/region gcloud config;
  - prompt user.

  Args:
    args: Namespace, The args namespace.
    prompt: bool, whether to attempt to prompt.

  Returns:
    A str representing region.
  """
  if getattr(args, 'region', None):
    return args.region
  if properties.VALUES.run.region.IsExplicitlySet():
    return properties.VALUES.run.region.Get()
  if properties.VALUES.compute.region.IsExplicitlySet():
    return properties.VALUES.compute.region.Get()
  if prompt:
    region = PromptForRegion()
    if region:
      # set the region on args, so we're not embarassed the next time we call
      # GetRegion
      args.region = region
      return region


def GetEndpointVisibility(args):
  """Return bool for explicitly set connectivity or None if not set."""
  if args.connectivity == 'internal':
    return True
  if args.connectivity == 'external':
    return False
  return None


def GetAllowUnauthenticated(args, client=None, service_ref=None, prompt=False):
  """Return bool for the explicit intent to allow unauth invocations or None.

  If --[no-]allow-unauthenticated is set, return that value. If not set,
  prompt for value if desired. If prompting not necessary or doable,
  return None, indicating that no action needs to be taken.

  Args:
    args: Namespace, The args namespace
    client: from googlecloudsdk.command_lib.run import serverless_operations
      serverless_operations.ServerlessOperations object
    service_ref: service resource reference (e.g. args.CONCEPTS.service.Parse())
    prompt: bool, whether to attempt to prompt.

  Returns:
    bool indicating whether to allow/unallow unauthenticated or None if N/A
  """
  if getattr(args, 'allow_unauthenticated', None) is not None:
    return args.allow_unauthenticated

  if prompt:
    if client is None or service_ref is None:
      raise ValueError(
          'A client and service reference are required for determining if the '
          'service\'s IAM policy binding can be modified.')
    if client.CanSetIamPolicyBinding(service_ref):
      return console_io.PromptContinue(
          prompt_string=('Allow unauthenticated invocations '
                         'to [{}]'.format(service_ref.servicesId)),
          default=False)
    else:
      pretty_print.Info(
          'This service will require authentication to be invoked.')
  return None


def GetKubeconfig(args):
  """Get config from kubeconfig file.

  Get config from potentially 3 different places, falling back to the next
  option as necessary:
  1. file_path specified as argument by the user
  2. List of file paths specified in $KUBECONFIG
  3. Default config path (~/.kube/config)

  Args:
    args: Namespace, The args namespace.

  Returns:
    dict: config object

  Raises:
    KubeconfigError: if $KUBECONFIG is set but contains no valid paths
  """
  if getattr(args, 'kubeconfig', None):
    return kubeconfig.Kubeconfig.LoadFromFile(
        files.ExpandHomeDir(args.kubeconfig))
  if os.getenv('KUBECONFIG'):
    config_paths = os.getenv('KUBECONFIG').split(os.pathsep)
    config = None
    # Merge together all valid paths into single config
    for path in config_paths:
      try:
        other_config = kubeconfig.Kubeconfig.LoadFromFile(
            files.ExpandHomeDir(path))
        if not config:
          config = other_config
        else:
          config.Merge(other_config)
      except kubeconfig.Error:
        pass
    if not config:
      raise KubeconfigError('No valid file paths found in $KUBECONFIG')
    return config
  return kubeconfig.Kubeconfig.LoadFromFile(
      files.ExpandHomeDir(_DEFAULT_KUBECONFIG_PATH))


def _FlagIsExplicitlySet(args, flag):
  """Return True if --flag is explicitly passed by the user."""
  return hasattr(args, flag) and args.IsSpecified(flag)


def VerifyOnePlatformFlags(args):
  """Raise ConfigurationError if args includes GKE only arguments."""
  error_msg = ('The `{flag}` flag is not supported on the fully managed '
               'version of Cloud Run. Specify `--platform {platform}` or run '
               '`gcloud config set run/platform {platform}` to work with '
               '{platform_desc}.')

  if _FlagIsExplicitlySet(args, 'connectivity'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--connectivity=[internal|external]',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'cpu'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--cpu',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'namespace'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--namespace',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'cluster'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--cluster',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'cluster_location'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--cluster-location',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'kubeconfig'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--kubeconfig',
            platform='kubernetes',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['kubernetes']))

  if _FlagIsExplicitlySet(args, 'context'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--context',
            platform='kubernetes',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['kubernetes']))


def VerifyGKEFlags(args):
  """Raise ConfigurationError if args includes OnePlatform only arguments."""
  error_msg = ('The `{flag}` flag is not supported with Cloud Run on GKE. '
               'Specify `--platform {platform}` or run `gcloud config set '
               'run/platform {platform}` to work with {platform_desc}.')

  if _FlagIsExplicitlySet(args, 'allow_unauthenticated'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--allow-unauthenticated',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'service_account'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--service-account',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'region'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--region',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'revision_suffix'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--revision-suffix',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'vpc_connector'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--vpc-connector',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'clear_vpc_connector'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--clear-vpc-connector',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'kubeconfig'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--kubeconfig',
            platform='kubernetes',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['kubernetes']))

  if _FlagIsExplicitlySet(args, 'context'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--context',
            platform='kubernetes',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['kubernetes']))


def VerifyKubernetesFlags(args):
  """Raise ConfigurationError if args includes OnePlatform or GKE only arguments."""
  error_msg = ('The `{flag}` flag is not supported when connecting to a '
               'Kubenetes cluster. Specify `--platform {platform}` or run '
               '`gcloud config set run/platform {platform}` to work with '
               '{platform_desc}.')

  if _FlagIsExplicitlySet(args, 'allow_unauthenticated'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--allow-unauthenticated',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'service_account'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--service-account',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'region'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--region',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'revision_suffix'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--revision-suffix',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'vpc_connector'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--vpc-connector',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'clear_vpc_connector'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--clear-vpc-connector',
            platform='managed',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['managed']))

  if _FlagIsExplicitlySet(args, 'cluster'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--cluster',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))

  if _FlagIsExplicitlySet(args, 'cluster_location'):
    raise serverless_exceptions.ConfigurationError(
        error_msg.format(
            flag='--cluster-location',
            platform='gke',
            platform_desc=_PLATFORM_SHORT_DESCRIPTIONS['gke']))


def GetPlatformFallback():
  """Fallback to accessing the property for declaritive commands."""
  return properties.VALUES.run.platform.Get()


def GetPlatform(args):
  """Returns the platform to run on."""
  platform = properties.VALUES.run.platform.Get()
  choices = args.GetFlagArgument('platform').choices_help
  if platform is None:
    if console_io.CanPrompt():
      platforms = sorted(choices.keys())
      idx = console_io.PromptChoice(
          platforms,
          message='Please choose a target platform:',
          cancel_option=True)
      platform = platforms[idx]
      # Set platform so we don't re-prompt on future calls to this method
      properties.VALUES.run.platform.Set(platform)
      log.status.Print(
          'To specify the platform yourself, pass `--platform {0}`. '
          'Or, to make this the default target platform, run '
          '`gcloud config set run/platform {0}`.\n'.format(platform))
    else:
      raise ArgumentError(
          'No platform specified. Pass the `--platform` flag or set '
          'the [run/platform] property to specify a target platform.\n'
          'Available platforms:\n{}'.format(
              '\n'.join(
                  ['- {}: {}'.format(k, v) for k, v in choices.items()])))

  if platform == 'managed':
    VerifyOnePlatformFlags(args)
  elif platform == 'gke':
    VerifyGKEFlags(args)
  else:
    raise ArgumentError(
        'Invalid target platform specified: [{}].\n'
        'Available platforms:\n{}'.format(
            platform,
            '\n'.join(['- {}: {}'.format(k, v) for k, v in choices.items()
                      ])))
  return platform


def IsKubernetes(args):
  """Returns True if args property specify Kubernetes.

  Args:
    args: Namespace, The args namespace.
  """
  return GetPlatform(args) == 'kubernetes'


def IsGKE(args):
  """Returns True if args properly specify GKE.

  Args:
    args: Namespace, The args namespace.
  """
  return GetPlatform(args) == 'gke'


def IsManaged(args):
  """Returns True if args properly specify managed.

  Args:
    args: Namespace, The args namespace.
  """
  return GetPlatform(args) == 'managed'


def ValidatePlatformIsManaged(platform):
  if platform != 'managed':
    raise calliope_exceptions.BadArgumentException(
        '--platform', 'The platform [{}] is not supported by this operation. '
        'Specify `--platform managed` or run '
        '`gcloud config set run/platform managed`.'.format(platform))
  return platform
