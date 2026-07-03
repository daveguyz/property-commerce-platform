<?php
if (!defined('ABSPATH')) exit;

/**
 * Shortcode → PCP.mount() bridge.
 *
 * Each shortcode outputs a container div plus an inline mount script.
 * The SDK itself is enqueued once per page by PCP_Assets (only on pages
 * that actually contain a PCP shortcode or block).
 */
class PCP_Shortcodes {

    public static function init() {
        add_shortcode('pcp_auction_listing', [__CLASS__, 'auction_listing']);
        add_shortcode('pcp_auction_room',    [__CLASS__, 'auction_room']);
        add_shortcode('pcp_account',         [__CLASS__, 'account']);
    }

    private static function container($widget, $props = []) {
        $id = 'pcp-' . $widget . '-' . wp_unique_id();
        $props_json = wp_json_encode((object) $props);
        PCP_Assets::mark_needed(); // ensures SDK is enqueued
        $mount = "PCP.mount('" . esc_js($widget) . "', '#" . esc_js($id) . "', {$props_json});";
        return "<div id=\"" . esc_attr($id) . "\"></div>"
             . "<script>document.addEventListener('pcp:ready',function(){{$mount}});"
             . "if(window.PCP){{$mount}}</script>";
    }

    public static function auction_listing($atts) {
        $atts = shortcode_atts(['status' => '', 'room_url' => ''], $atts);
        $props = [];
        if ($atts['status'])   $props['statuses'] = array_map('trim', explode(',', $atts['status']));
        // Lot links point to the configured auction-room page
        $room = $atts['room_url'] ?: get_option('pcp_room_page', '/auction-room/');
        $props['roomUrl'] = trailingslashit(home_url($room)) . '?lot={lotId}';
        return self::container('auction-listing', $props);
    }

    public static function auction_room($atts) {
        $atts = shortcode_atts(['lot_id' => ''], $atts);
        $props = [];
        if ($atts['lot_id']) {
            $props['lotId'] = $atts['lot_id'];
        } else {
            // Read ?lot= from the URL client-side
            return "<div id=\"pcp-auction-room-auto\"></div>"
                 . "<script>document.addEventListener('pcp:ready',function(){"
                 . "var l=new URLSearchParams(location.search).get('lot');"
                 . "if(l)PCP.mount('auction-room','#pcp-auction-room-auto',{lotId:l});"
                 . "});</script>" . PCP_Assets::mark_needed_and_return('');
        }
        return self::container('auction-room', $props);
    }

    public static function account() {
        return self::container('account', []);
    }
}
