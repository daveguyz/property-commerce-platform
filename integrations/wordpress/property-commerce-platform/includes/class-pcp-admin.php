<?php
if (!defined('ABSPATH')) exit;

/**
 * Settings → Property Commerce admin page.
 * The API key is stored encrypted-at-rest using WP salts (best effort —
 * true secrecy requires a secrets manager, but this beats plaintext).
 */
class PCP_Admin {

    public static function init() {
        add_action('admin_menu',  [__CLASS__, 'add_menu']);
        add_action('admin_init',  [__CLASS__, 'register_settings']);
    }

    public static function add_menu() {
        add_options_page(
            'Property Commerce Platform',
            'Property Commerce',
            'manage_options',
            'pcp-settings',
            [__CLASS__, 'render_page']
        );
    }

    public static function register_settings() {
        register_setting('pcp_settings', 'pcp_api_url',   ['sanitize_callback' => 'esc_url_raw']);
        register_setting('pcp_settings', 'pcp_tenant_id', ['sanitize_callback' => 'sanitize_text_field']);
        register_setting('pcp_settings', 'pcp_api_key',   ['sanitize_callback' => [__CLASS__, 'encrypt_key']]);
        register_setting('pcp_settings', 'pcp_room_page', ['sanitize_callback' => 'sanitize_text_field']);
    }

    public static function encrypt_key($value) {
        if (empty($value)) return get_option('pcp_api_key'); // keep existing on blank submit
        if (str_starts_with($value, 'enc:')) return $value;   // already encrypted
        $cipher = openssl_encrypt($value, 'aes-256-cbc', wp_salt('auth'), 0, substr(wp_salt('secure_auth'), 0, 16));
        return 'enc:' . $cipher;
    }

    public static function decrypt_key() {
        $stored = get_option('pcp_api_key', '');
        if (!str_starts_with($stored, 'enc:')) return $stored;
        return openssl_decrypt(substr($stored, 4), 'aes-256-cbc', wp_salt('auth'), 0, substr(wp_salt('secure_auth'), 0, 16));
    }

    public static function render_page() {
        if (!current_user_can('manage_options')) return;

        // Test-connection handler
        $test_result = null;
        if (isset($_POST['pcp_test_connection']) && check_admin_referer('pcp_test')) {
            $test_result = self::test_connection();
        }
        ?>
        <div class="wrap">
            <h1>Property Commerce Platform</h1>
            <?php if ($test_result !== null): ?>
                <div class="notice notice-<?php echo $test_result['ok'] ? 'success' : 'error'; ?>">
                    <p><?php echo esc_html($test_result['message']); ?></p>
                </div>
            <?php endif; ?>

            <form method="post" action="options.php">
                <?php settings_fields('pcp_settings'); ?>
                <table class="form-table">
                    <tr>
                        <th><label for="pcp_api_url">Platform API URL</label></th>
                        <td><input type="url" id="pcp_api_url" name="pcp_api_url" class="regular-text"
                                   value="<?php echo esc_attr(get_option('pcp_api_url')); ?>"></td>
                    </tr>
                    <tr>
                        <th><label for="pcp_tenant_id">Tenant ID</label></th>
                        <td><input type="text" id="pcp_tenant_id" name="pcp_tenant_id" class="regular-text"
                                   value="<?php echo esc_attr(get_option('pcp_tenant_id')); ?>">
                            <p class="description">From your dashboard at app.propertycommerce.io</p></td>
                    </tr>
                    <tr>
                        <th><label for="pcp_api_key">API Key</label></th>
                        <td><input type="password" id="pcp_api_key" name="pcp_api_key" class="regular-text"
                                   placeholder="<?php echo get_option('pcp_api_key') ? '•••••••• (saved)' : 'pcp_live_...'; ?>">
                            <p class="description">Stored encrypted. Leave blank to keep the current key.</p></td>
                    </tr>
                    <tr>
                        <th><label for="pcp_room_page">Auction room page</label></th>
                        <td><input type="text" id="pcp_room_page" name="pcp_room_page" class="regular-text"
                                   value="<?php echo esc_attr(get_option('pcp_room_page', '/auction-room/')); ?>">
                            <p class="description">Page containing the [pcp_auction_room] shortcode. Lot links point here.</p></td>
                    </tr>
                </table>
                <?php submit_button(); ?>
            </form>

            <form method="post">
                <?php wp_nonce_field('pcp_test'); ?>
                <p><button type="submit" name="pcp_test_connection" class="button">Test connection</button></p>
            </form>

            <h2>Shortcodes</h2>
            <table class="widefat" style="max-width:720px">
                <thead><tr><th>Shortcode</th><th>Renders</th></tr></thead>
                <tbody>
                    <tr><td><code>[pcp_auction_listing]</code></td><td>Auction listing grid with Live/Upcoming/Closed tabs</td></tr>
                    <tr><td><code>[pcp_auction_room lot_id="uuid"]</code></td><td>Live bidding room (lot_id optional — reads ?lot= from URL)</td></tr>
                    <tr><td><code>[pcp_account]</code></td><td>Bidder account: deposits, wins, agreements</td></tr>
                </tbody>
            </table>
        </div>
        <?php
    }

    private static function test_connection() {
        $api_url = get_option('pcp_api_url');
        $tenant  = get_option('pcp_tenant_id');
        if (empty($api_url) || empty($tenant)) {
            return ['ok' => false, 'message' => 'Set the API URL and Tenant ID first.'];
        }
        $resp = wp_remote_get("{$api_url}/api/v1/tenants/{$tenant}/config", ['timeout' => 10]);
        if (is_wp_error($resp)) {
            return ['ok' => false, 'message' => 'Connection failed: ' . $resp->get_error_message()];
        }
        $code = wp_remote_retrieve_response_code($resp);
        if ($code === 200) {
            $body = json_decode(wp_remote_retrieve_body($resp), true);
            $name = $body['data']['name'] ?? 'unknown';
            return ['ok' => true, 'message' => "Connected — tenant \"{$name}\" found."];
        }
        return ['ok' => false, 'message' => "Platform returned HTTP {$code}. Check the Tenant ID."];
    }
}
