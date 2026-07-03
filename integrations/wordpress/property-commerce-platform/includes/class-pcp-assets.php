<?php
if (!defined('ABSPATH')) exit;

/**
 * Enqueues the bundled SDK only on pages that use a PCP shortcode/block.
 * The PCPConfig global is printed before the SDK script so config.js
 * picks it up on load. The API key is deliberately NOT exposed to the
 * browser — anonymous browsing uses the public tenant config endpoint;
 * bidder actions require an end-user login (JWT) handled by the SDK.
 */
class PCP_Assets {

    private static $needed = false;

    public static function init() {
        add_action('wp_enqueue_scripts', [__CLASS__, 'maybe_enqueue'], 20);
        // Detect shortcode presence early so enqueue can be conditional
        add_action('wp', [__CLASS__, 'detect_shortcodes']);
    }

    public static function mark_needed() { self::$needed = true; }
    public static function mark_needed_and_return($v) { self::$needed = true; return $v; }

    public static function detect_shortcodes() {
        if (!is_singular()) return;
        $post = get_post();
        if (!$post) return;
        foreach (['pcp_auction_listing', 'pcp_auction_room', 'pcp_account'] as $sc) {
            if (has_shortcode($post->post_content, $sc)) { self::$needed = true; return; }
        }
        if (has_block('property-commerce/auction-listing', $post)
            || has_block('property-commerce/auction-room', $post)) {
            self::$needed = true;
        }
    }

    public static function maybe_enqueue() {
        if (!self::$needed) return;

        wp_enqueue_script(
            'pcp-sdk',
            PCP_PLUGIN_URL . 'assets/pcp.min.js',
            [],
            PCP_PLUGIN_VERSION,
            ['in_footer' => true]
        );

        $config = [
            'apiUrl'   => get_option('pcp_api_url', ''),
            'wsUrl'    => str_replace(['https://', 'http://'], ['wss://', 'ws://'],
                              get_option('pcp_api_url', '')) . '/ws',
            'tenantId' => get_option('pcp_tenant_id', ''),
            'locale'   => substr(get_locale(), 0, 2),
        ];
        wp_add_inline_script('pcp-sdk',
            'window.PCPConfig = ' . wp_json_encode($config) . ';',
            'before');
    }
}
