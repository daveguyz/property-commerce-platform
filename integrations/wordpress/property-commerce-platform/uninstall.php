<?php
if (!defined('WP_UNINSTALL_PLUGIN')) exit;
delete_option('pcp_api_url');
delete_option('pcp_tenant_id');
delete_option('pcp_api_key');
delete_option('pcp_room_page');
