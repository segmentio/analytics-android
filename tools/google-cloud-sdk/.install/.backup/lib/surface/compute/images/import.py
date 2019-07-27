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
"""Import image command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import abc
import os.path
import string
import uuid

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import daisy_utils
from googlecloudsdk.api_lib.compute import image_utils
from googlecloudsdk.api_lib.compute import utils
from googlecloudsdk.api_lib.storage import storage_api
from googlecloudsdk.api_lib.storage import storage_util
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.command_lib.compute.images import flags
from googlecloudsdk.command_lib.compute.images import os_choices
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources
from googlecloudsdk.core.console import progress_tracker
import six

_WORKFLOW_DIR = '../workflows/image_import/'
_IMPORT_WORKFLOW = _WORKFLOW_DIR + 'import_image.wf.json'
_IMPORT_FROM_IMAGE_WORKFLOW = _WORKFLOW_DIR + 'import_from_image.wf.json'
_IMPORT_AND_TRANSLATE_WORKFLOW = _WORKFLOW_DIR + 'import_and_translate.wf.json'
_WORKFLOWS_URL = ('https://github.com/GoogleCloudPlatform/compute-image-tools/'
                  'tree/master/daisy_workflows/image_import')
_OUTPUT_FILTER = ['[Daisy', '[import-', 'starting build', '  import', 'ERROR']


def _IsLocalFile(file_name):
  return not (file_name.startswith('gs://') or
              file_name.startswith('https://'))


def _UploadToGcsGsutil(local_path, dest_path):
  """Uploads a local file to GCS using gsutil."""
  retcode = storage_util.RunGsutilCommand('cp', [local_path, dest_path])
  if retcode != 0:
    log.err.Print('Failed to upload file. See {} for details.'.format(
        log.GetLogFilePath()))
    raise exceptions.FailedSubCommand(
        ['gsutil', 'cp', local_path, dest_path], retcode)
  return dest_path


def _GetTranslateWorkflow(args):
  if args.os:
    return os_choices.OS_CHOICES_MAP[args.os]
  return args.custom_workflow


def _AppendTranslateWorkflowArg(args, import_args):
  if args.os:
    daisy_utils.AppendArg(import_args, 'os', args.os)
  daisy_utils.AppendArg(import_args, 'custom_translate_workflow',
                        args.custom_workflow)


def _CheckImageName(image_name):
  """Checks for a valid GCE image name."""
  name_message = ('Name must start with a lowercase letter followed by up to '
                  '63 lowercase letters, numbers, or hyphens, and cannot end '
                  'with a hyphen.')
  name_ok = True
  valid_chars = string.digits + string.ascii_lowercase + '-'
  if len(image_name) > 64:
    name_ok = False
  elif image_name[0] not in string.ascii_lowercase:
    name_ok = False
  elif not all(char in valid_chars for char in image_name):
    name_ok = False
  elif image_name[-1] == '-':
    name_ok = False

  if not name_ok:
    raise exceptions.InvalidArgumentException('IMAGE_NAME', name_message)


def _CheckForExistingImage(image_name, compute_holder):
  """Check that the destination image does not already exist."""
  _CheckImageName(image_name)
  image_ref = resources.REGISTRY.Parse(
      image_name,
      collection='compute.images',
      params={'project': properties.VALUES.core.project.GetOrFail})

  image_expander = image_utils.ImageExpander(compute_holder.client,
                                             compute_holder.resources)
  try:
    _ = image_expander.GetImage(image_ref)
    image_exists = True
  except utils.ImageNotFoundError:
    image_exists = False

  if image_exists:
    message = 'The image [{0}] already exists.'.format(image_name)
    raise exceptions.InvalidArgumentException('IMAGE_NAME', message)


@base.ReleaseTracks(base.ReleaseTrack.GA)
class Import(base.CreateCommand):
  """Import an image into Google Compute Engine."""

  _OS_CHOICES = os_choices.OS_CHOICES_IMAGE_IMPORT_GA

  def __init__(self, *args, **kwargs):
    self.storage_client = storage_api.StorageClient()
    super(Import, self).__init__(*args, **kwargs)

  @classmethod
  def Args(cls, parser):
    Import.DISK_IMAGE_ARG = flags.MakeDiskImageArg()
    Import.DISK_IMAGE_ARG.AddArgument(parser, operation_type='create')

    flags.compute_flags.AddZoneFlag(
        parser, 'image', 'import',
        explanation='The zone in which to do the work of importing the image.')

    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument(
        '--source-file',
        help=("""A local file, or the Google Cloud Storage URI of the virtual
              disk file to import. For example: ``gs://my-bucket/my-image.vmdk''
              or ``./my-local-image.vmdk''"""),
    )
    flags.SOURCE_IMAGE_ARG.AddArgument(source, operation_type='import')

    workflow = parser.add_mutually_exclusive_group(required=True)
    workflow.add_argument(
        '--os',
        choices=sorted(cls._OS_CHOICES),
        help='Specifies the OS of the image being imported.'
    )
    workflow.add_argument(
        '--data-disk',
        help=('Specifies that the disk has no bootable OS installed on it. '
              'Imports the disk without making it bootable or installing '
              'Google tools on it.'),
        action='store_true'
    )
    workflow.add_argument(
        '--custom-workflow',
        help=("""\
              Specifies a custom workflow to use for image translation.
              Workflow should be relative to the image_import directory here:
              []({0}). For example: ``{1}''""".format(
                  _WORKFLOWS_URL,
                  os_choices.OS_CHOICES_MAP[sorted(cls._OS_CHOICES)[0]])),
        hidden=True
    )

    daisy_utils.AddCommonDaisyArgs(parser)

    parser.add_argument(
        '--guest-environment',
        action='store_true',
        default=True,
        help='Google Guest Environment will be installed on the image.')

    parser.add_argument(
        '--network',
        help=('Name of the network in your project to use for the image import.'
              ' The network must have access to Google Cloud Storage. If not '
              'specified, the network named `default` is used.'),
    )

    parser.add_argument(
        '--subnet',
        help=('Name of the subnetwork in your project to use for the image '
              'import. If the network resource is in legacy mode, do not '
              'provide this property. If the network is in auto subnet mode, '
              'providing the subnetwork is optional. If the network is in '
              'custom subnet mode, then this field should be specified. '
              'Region or zone should be specified if this field is specified.'),
    )

    parser.display_info.AddCacheUpdater(flags.ImagesCompleter)

  def Run(self, args):
    compute_holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    # Fail early if the requested image name is invalid or already exists.
    _CheckImageName(args.image_name)
    _CheckForExistingImage(args.image_name, compute_holder)

    import_stager = self._CreateImportStager(args)
    import_metadata = self._Stage(import_stager)

    # TODO(b/79591894): Once we've cleaned up the Argo output, replace this
    # warning message with a ProgressTracker spinner.
    log.warning('Importing image. This may take up to 2 hours.')
    tags = ['gce-daisy-image-import']

    return self._RunImageImport(args, import_stager, import_metadata, tags)

  def _Stage(self, import_stager):
    """Prepares for import.

    Args:
      import_stager: BaseImportStager to do the actual staging job.

    Returns:
      list of str, which contains metadata for the import step
    """
    return import_stager.StageForDaisy()

  def _RunImageImport(self, args, import_stager, import_metadata, tags):
    """Run actual image import.

    Args:
      args: list of str, CLI args that might contain network/subnet args.
      import_stager: BaseImportStager, to do actual stage steps.
      import_metadata: list of str, contains metadata used by import. It can be
        daisy vars or import wrapper args.
      tags: A list of strings for adding tags to the Argo build.

    Returns:
      A cloud build that executes importing.
    """
    return daisy_utils.RunDaisyBuild(
        args,
        import_stager.GetDaisyWorkflow(),
        ','.join(import_metadata),
        tags=tags,
        daisy_bucket=import_stager.GetDaisyBucket(),
        user_zone=properties.VALUES.compute.zone.Get(),
        output_filter=_OUTPUT_FILTER)

  def _MakeGcsUri(self, uri):
    return daisy_utils.MakeGcsUri(uri)

  def _CreateImportStager(self, args):
    if args.source_image:
      return ImportFromImageStager(self.storage_client, args)
    elif _IsLocalFile(args.source_file):
      return ImportFromLocalFileStager(self.storage_client, args)
    else:
      return ImportFromGSFileStager(self.storage_client, args, self._MakeGcsUri)


@six.add_metaclass(abc.ABCMeta)
class BaseImportStager(object):
  """Base class for image import stager.

  An abstract class which is responsible for preparing import parameters, such
  as Daisy parameters and workflow, as well as creating Daisy scratch bucket in
  the appropriate location.
  """

  def __init__(self, storage_client, args):
    self.storage_client = storage_client
    self.args = args

    self._CreateDaisyBucket()

  def _CreateDaisyBucket(self):
    # Create Daisy bucket in default GS location (US Multi-regional)
    # This is default behaviour for all types of import except from a file in GS
    self.daisy_bucket = self.GetAndCreateDaisyBucket()

  def GetDaisyBucket(self):
    """Returns the name of Daisy scratch bucket.

    Returns:
      A string. Name of the Daisy scratch bucket used for running import.
    """
    return self.daisy_bucket

  def StageForDaisy(self):
    """Prepares import for execution and returns daisy variables.

    It supports native daisy, which is on the path of deprecation for image
    import feature.

    Returns:
      daisy_vars - array of strings, Daisy variables.
    """
    daisy_vars = []
    self._BuildDaisyVars(daisy_vars)
    return daisy_vars

  def StageForImporter(self):
    """Prepares import for execution and returns import variables.

    It supports running new import wrapper (gce_vm_image_import).

    Returns:
      import_args - array of strings, import variables.
    """
    import_args = []
    self._BuildImportArgs(import_args)
    return import_args

  def _BuildDaisyVars(self, daisy_vars):
    daisy_vars.append('image_name={}'.format(self.args.image_name))

    if not self.args.guest_environment:
      daisy_vars.append('install_gce_packages={}'.format('false'))

    daisy_vars.extend(daisy_utils.ExtractNetworkAndSubnetDaisyVariables(
        self.args, daisy_utils.ImageOperation.IMPORT))

  def _BuildImportArgs(self, import_args):
    """Build args to support running new import wrapper - gce_vm_image_import.

    Args:
      import_args: array of str, args to build.
    """
    daisy_utils.AppendArg(import_args, 'zone',
                          properties.VALUES.compute.zone.Get())
    daisy_utils.AppendArg(import_args, 'scratch_bucket_gcs_path',
                          'gs://{0}/'.format(self.GetDaisyBucket()))
    daisy_utils.AppendArg(import_args, 'timeout',
                          '{}s'.format(daisy_utils.GetDaisyTimeout(self.args)))

    daisy_utils.AppendArg(import_args, 'client_id', 'gcloud')
    daisy_utils.AppendArg(import_args, 'image_name', self.args.image_name)
    daisy_utils.AppendBoolArg(import_args, 'no_guest_environment',
                              not self.args.guest_environment)
    daisy_utils.AppendNetworkAndSubnetArgs(self.args, import_args)

  @abc.abstractmethod
  def GetDaisyWorkflow(self):
    raise NotImplementedError

  def GetAndCreateDaisyBucket(self):
    bucket_name = daisy_utils.GetDaisyBucketName()
    self.storage_client.CreateBucketIfNotExists(bucket_name)
    return bucket_name


class ImportFromImageStager(BaseImportStager):
  """Image import stager from an existing image."""

  def _BuildDaisyVars(self, daisy_vars):
    super(ImportFromImageStager, self)._BuildDaisyVars(daisy_vars)
    daisy_vars.append(
        'translate_workflow={}'.format(_GetTranslateWorkflow(self.args)))

    source_name = self._GetSourceImage()
    daisy_vars.append('source_image={}'.format(source_name))

  def _BuildImportArgs(self, import_args):
    daisy_utils.AppendArg(import_args, 'source_image', self.args.source_image)
    _AppendTranslateWorkflowArg(self.args, import_args)
    super(ImportFromImageStager, self)._BuildImportArgs(import_args)

  def _GetSourceImage(self):
    ref = resources.REGISTRY.Parse(
        self.args.source_image, collection='compute.images',
        params={'project': properties.VALUES.core.project.GetOrFail})
    # source_name should be of the form 'global/images/image-name'.
    source_name = ref.RelativeName()[len(ref.Parent().RelativeName() + '/'):]
    return source_name

  def GetDaisyWorkflow(self):
    return _IMPORT_FROM_IMAGE_WORKFLOW


class BaseImportFromFileStager(BaseImportStager):
  """Abstract image import stager for import from a file."""

  def _BuildDaisyVars(self, daisy_vars):
    super(BaseImportFromFileStager, self)._BuildDaisyVars(daisy_vars)
    # Import and (maybe) translate from the scratch bucket.
    daisy_vars.append('source_disk_file={}'.format(self.gcs_uri))
    if not self.args.data_disk:
      daisy_vars.append(
          'translate_workflow={}'.format(_GetTranslateWorkflow(self.args)))

  def _BuildImportArgs(self, import_args):
    # Import and (maybe) translate from the scratch bucket.
    daisy_utils.AppendArg(import_args, 'source_file', self.gcs_uri)
    if self.args.data_disk:
      daisy_utils.AppendBoolArg(import_args, 'data_disk', self.args.data_disk)
    else:
      _AppendTranslateWorkflowArg(self.args, import_args)

    super(BaseImportFromFileStager, self)._BuildImportArgs(import_args)

  def GetDaisyWorkflow(self):
    if self.args.data_disk:
      return _IMPORT_WORKFLOW
    else:
      return _IMPORT_AND_TRANSLATE_WORKFLOW

  def StageForDaisy(self):
    self._FileStage()
    return super(BaseImportFromFileStager, self).StageForDaisy()

  def StageForImporter(self):
    self._FileStage()
    return super(BaseImportFromFileStager, self).StageForImporter()

  def _FileStage(self):
    """Prepare image file for importing."""
    # If the file is an OVA file, print a warning.
    if self.args.source_file.endswith('.ova'):
      log.warning(
          'The specified input file may contain more than one virtual disk. '
          'Only the first vmdk disk will be imported.')
    elif (self.args.source_file.endswith('.tar.gz')
          or self.args.source_file.endswith('.tgz')):
      raise exceptions.BadFileException(
          '`gcloud compute images import` does not support compressed '
          'archives. Please extract your image and try again.\n If you got '
          'this file by exporting an image from Compute Engine (e.g. by '
          'using `gcloud compute images export`) then you can instead use '
          '`gcloud compute images create` to create your image from your '
          '.tar.gz file.')
    self.gcs_uri = self._CopySourceFileToScratchBucket()

  @abc.abstractmethod
  def _CopySourceFileToScratchBucket(self):
    raise NotImplementedError


class ImportFromLocalFileStager(BaseImportFromFileStager):
  """Image import stager from a local file."""

  def _CopySourceFileToScratchBucket(self):
    return self._UploadToGcs(
        self.args.async, self.args.source_file, self.daisy_bucket, uuid.uuid4())

  def _UploadToGcs(self, is_async, local_path, daisy_bucket, image_uuid):
    """Uploads a local file to GCS. Returns the gs:// URI to that file."""
    file_name = os.path.basename(local_path).replace(' ', '-')
    dest_path = 'gs://{0}/tmpimage/{1}-{2}'.format(
        daisy_bucket, image_uuid, file_name)
    if is_async:
      log.status.Print('Async: Once upload is complete, your image will be '
                       'imported from Cloud Storage asynchronously.')
    with progress_tracker.ProgressTracker(
        'Copying [{0}] to [{1}]'.format(local_path, dest_path)):
      # TODO(b/109938541): Remove gsutil implementation after the new
      # implementation seems stable.
      use_gsutil = properties.VALUES.storage.use_gsutil.GetBool()
      if use_gsutil:
        return _UploadToGcsGsutil(local_path, dest_path)
      else:
        return self._UploadToGcsStorageApi(local_path, dest_path)

  def _UploadToGcsStorageApi(self, local_path, dest_path):
    """Uploads a local file to GCS using the gcloud storage api client."""
    dest_object = storage_util.ObjectReference.FromUrl(dest_path)
    self.storage_client.CopyFileToGCS(local_path, dest_object)
    return dest_path


class ImportFromGSFileStager(BaseImportFromFileStager):
  """Image import stager from a file in GCS."""

  def __init__(self, storage_client, args, makeGcsUri):
    self.source_file_gcs_uri = makeGcsUri(args.source_file)
    super(ImportFromGSFileStager, self).__init__(storage_client, args)

  def GetAndCreateDaisyBucket(self):
    bucket_location = self.storage_client.GetBucketLocationForFile(
        self.source_file_gcs_uri)
    bucket_name = daisy_utils.GetDaisyBucketName(bucket_location)
    self.storage_client.CreateBucketIfNotExists(
        bucket_name, location=bucket_location)
    return bucket_name

  def _CopySourceFileToScratchBucket(self):
    image_file = os.path.basename(self.source_file_gcs_uri)
    dest_uri = 'gs://{0}/tmpimage/{1}-{2}'.format(
        self.daisy_bucket, uuid.uuid4(), image_file)
    src_object = resources.REGISTRY.Parse(self.source_file_gcs_uri,
                                          collection='storage.objects')
    dest_object = resources.REGISTRY.Parse(dest_uri,
                                           collection='storage.objects')
    with progress_tracker.ProgressTracker(
        'Copying [{0}] to [{1}]'.format(self.source_file_gcs_uri, dest_uri)):
      self.storage_client.Rewrite(src_object, dest_object)
    return dest_uri


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class ImportBeta(Import):
  """Import an image into Google Compute Engine for Beta releases."""

  _OS_CHOICES = os_choices.OS_CHOICES_IMAGE_IMPORT_BETA

  def _MakeGcsUri(self, uri):
    try:
      return daisy_utils.MakeGcsObjectOrPathUri(uri)
    except storage_util.InvalidObjectNameError:
      raise exceptions.InvalidArgumentException(
          'source-file', 'must be a path to an object in Google Cloud Storage')

  def _Stage(self, import_stager):
    return import_stager.StageForImporter()

  def _RunImageImport(self, args, import_stager, import_metadata, tags):
    return daisy_utils.RunImageImport(args, import_metadata, tags,
                                      _OUTPUT_FILTER)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class ImportAlpha(ImportBeta):
  """Import an image into Google Compute Engine for Alpha releases."""

  _OS_CHOICES = os_choices.OS_CHOICES_IMAGE_IMPORT_ALPHA


Import.detailed_help = {
    'brief': 'Import an image into Google Compute Engine',
    'DESCRIPTION': """\
        *{command}* imports Virtual Disk images, such as VMWare VMDK files
        and VHD files, into Google Compute Engine.

        Importing images involves 3 steps:
        *  Upload the virtual disk file to Google Cloud Storage.
        *  Import the image to Google Compute Engine.
        *  Translate the image to make a bootable image.
        This command will perform all three of these steps as necessary,
        depending on the input arguments specified by the user.

        This command uses the `--os` flag to choose the appropriate translation.
        You can omit the translation step using the `--data-disk` flag.

        If you exported your disk from Google Compute Engine then you do not
        need to re-import it. Instead, use the `create` command to create
        further images from it.

        Files stored on Cloud Storage and images in Compute Engine incur
        charges. See [](https://cloud.google.com/compute/docs/images/importing-virtual-disks#resource_cleanup).
        """,
}
