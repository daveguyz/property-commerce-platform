<?php
/**
 * Plugin Name:       Property Commerce Platform
 * Plugin URI:        https://propertycommerce.io/wordpress
 * Description:       Embed live property auctions, listings, bookings and account dashboards on your WordPress site, powered by the Property Commerce Platform.
 * Version:           2.0.0
 * Requires at least: 6.0
 * Requires PHP:      8.0
 * Author:            Property Commerce Platform
 * License:           Apache-2.0
 * Text Domain:       property-commerce-platform
 */

if (!defined('ABSPATH')) exit; // No direct access

define('PCP_PLUGIN_VERSION', '2.0.0');
define('PCP_PLUGIN_DIR', plugin_dir_path(__FILE__));
define('PCP_PLUGIN_URL', plugin_dir_url(__FILE__));

require_once PCP_PLUGIN_DIR . 'includes/class-pcp-admin.php';
require_once PCP_PLUGIN_DIR . 'includes/class-pcp-shortcodes.php';
require_once PCP_PLUGIN_DIR . 'includes/class-pcp-assets.php';
require_once PCP_PLUGIN_DIR . 'includes/class-pcp-block.php';

add_action('plugins_loaded', function () {
    PCP_Admin::init();
    PCP_Shortcodes::init();
    PCP_Assets::init();
    PCP_Block::init();
});

// Activation: seed default options
register_activation_hook(__FILE__, function () {
    add_option('pcp_api_url', 'https://api.propertycommerce.io');
    add_option('pcp_tenant_id', '');
    add_option('pcp_api_key', '');
    add_option('pcp_room_page', '/auction-room/');
});
