<?php
if (!defined('ABSPATH')) exit;

/**
 * Gutenberg blocks — server-rendered wrappers around the shortcodes so
 * the editor sidebar exposes the same options without duplicating the
 * mount logic.
 */
class PCP_Block {

    public static function init() {
        add_action('init', [__CLASS__, 'register_blocks']);
    }

    public static function register_blocks() {
        wp_register_script(
            'pcp-blocks-editor',
            PCP_PLUGIN_URL . 'assets/blocks.js',
            ['wp-blocks', 'wp-element', 'wp-components', 'wp-block-editor'],
            PCP_PLUGIN_VERSION,
            true
        );

        register_block_type('property-commerce/auction-listing', [
            'editor_script'   => 'pcp-blocks-editor',
            'render_callback' => function ($attrs) {
                $status = esc_attr($attrs['status'] ?? '');
                return do_shortcode("[pcp_auction_listing status=\"{$status}\"]");
            },
            'attributes' => [
                'status' => ['type' => 'string', 'default' => ''],
            ],
        ]);

        register_block_type('property-commerce/auction-room', [
            'editor_script'   => 'pcp-blocks-editor',
            'render_callback' => function ($attrs) {
                $lot = esc_attr($attrs['lotId'] ?? '');
                return do_shortcode("[pcp_auction_room lot_id=\"{$lot}\"]");
            },
            'attributes' => [
                'lotId' => ['type' => 'string', 'default' => ''],
            ],
        ]);
    }
}
