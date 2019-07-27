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
"""Constants/functions related to operating system options supported by image and OVF imports."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

OS_CHOICES_MAP = {
    'debian-8': 'debian/translate_debian_8.wf.json',
    'debian-9': 'debian/translate_debian_9.wf.json',
    'centos-6': 'enterprise_linux/translate_centos_6.wf.json',
    'centos-7': 'enterprise_linux/translate_centos_7.wf.json',
    'rhel-6': 'enterprise_linux/translate_rhel_6_licensed.wf.json',
    'rhel-7': 'enterprise_linux/translate_rhel_7_licensed.wf.json',
    'rhel-6-byol': 'enterprise_linux/translate_rhel_6_byol.wf.json',
    'rhel-7-byol': 'enterprise_linux/translate_rhel_7_byol.wf.json',
    'ubuntu-1404': 'ubuntu/translate_ubuntu_1404.wf.json',
    'ubuntu-1604': 'ubuntu/translate_ubuntu_1604.wf.json',
    'windows-2008r2': 'windows/translate_windows_2008_r2.wf.json',
    'windows-2012': 'windows/translate_windows_2012.wf.json',
    'windows-2012r2': 'windows/translate_windows_2012_r2.wf.json',
    'windows-2016': 'windows/translate_windows_2016.wf.json',
    'windows-2008r2-byol': 'windows/translate_windows_2008_r2_byol.wf.json',
    'windows-2012-byol': 'windows/translate_windows_2012_byol.wf.json',
    'windows-2012r2-byol': 'windows/translate_windows_2012_r2_byol.wf.json',
    'windows-2016-byol': 'windows/translate_windows_2016_byol.wf.json',
    'windows-7-byol': 'windows/translate_windows_7_byol.wf.json',
    'windows-10-byol': 'windows/translate_windows_10_byol.wf.json',
}

OS_CHOICES_IMAGE_IMPORT_GA = [
    'debian-8',
    'debian-9',
    'centos-6',
    'centos-7',
    'rhel-6',
    'rhel-7',
    'rhel-6-byol',
    'rhel-7-byol',
    'ubuntu-1404',
    'ubuntu-1604',
    'windows-2008r2',
    'windows-2012',
    'windows-2012r2',
    'windows-2016',
    'windows-2008r2-byol',
    'windows-2012-byol',
    'windows-2012r2-byol',
    'windows-2016-byol',
    'windows-7-byol',
    'windows-10-byol',
]
OS_CHOICES_IMAGE_IMPORT_BETA = OS_CHOICES_IMAGE_IMPORT_GA + []
OS_CHOICES_IMAGE_IMPORT_ALPHA = OS_CHOICES_IMAGE_IMPORT_BETA + []
OS_CHOICES_INSTANCE_IMPORT_BETA = OS_CHOICES_IMAGE_IMPORT_GA
