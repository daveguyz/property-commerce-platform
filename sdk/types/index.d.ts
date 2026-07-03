/**
 * Property Commerce Platform SDK — TypeScript definitions.
 * Ships with @property-commerce/sdk on npm. Also consumed by the
 * pcp-tools VS Code extension for pcp.config.json IntelliSense.
 */

export interface PCPTheme {
  /** Brand colour for buttons, active tabs, prices. Default '#6366f1'. */
  primaryColor?: string;
  /** Text colour on primary backgrounds. Default '#ffffff'. */
  primaryTextColor?: string;
  /** 'inherit' uses the host site's font stack. */
  fontFamily?: string;
  /** e.g. '8px'. Applied to cards, buttons, inputs. */
  borderRadius?: string;
  /** Tenant logo, populated from white-label config. */
  logoUrl?: string;
}

export interface PCPConfig {
  /** Platform API base URL. Omit to run in mock mode with sample data. */
  apiUrl?: string;
  /** WebSocket URL for live bidding. */
  wsUrl?: string;
  /** Your tenant UUID from app.propertycommerce.io. */
  tenantId?: string;
  /** Tenant API key — only for anonymous server-rendered embeds. */
  apiKey?: string;
  /** ISO 4217 code. Default 'USD'. */
  currency?: string;
  /** Two-letter locale. Default 'en'. */
  locale?: string;
  theme?: PCPTheme;
  /** Set automatically when apiUrl is omitted. */
  mockMode?: boolean;
}

export interface AuctionListingProps {
  /** Initial status filter. Default ['OPEN', 'EXTENDED']. */
  statuses?: Array<'OPEN' | 'EXTENDED' | 'SCHEDULED' | 'CLOSED' | 'SETTLED' | 'NO_RESERVE'>;
  /** Filter by auction type. */
  type?: 'ENGLISH' | 'DUTCH' | 'REVERSE' | 'SEALED_BID';
  /**
   * URL template for lot links. '{lotId}' is replaced per lot.
   * e.g. '/auction-room/?lot={lotId}'. Default '#lot={lotId}'.
   */
  roomUrl?: string;
}

export interface AuctionRoomProps {
  /** The lot to join. Required. */
  lotId: string;
}

export interface MountInstance {
  unmount(): void;
}

export interface PCPAuth {
  getToken(): string | null;
  setToken(token: string): void;
  getRefresh(): string | null;
  setRefresh(token: string): void;
  clear(): void;
  isAuthenticated(): boolean;
  getRoles(): string[];
  getUserId(): string | null;
  headers(): Record<string, string>;
}

export interface PCPI18n {
  /** Format a number in the configured currency: fmt(450000) → "$450,000". */
  fmt(amount: number): string;
  fmtDate(iso: string, opts?: { withTime?: boolean; withYear?: boolean; timezone?: string }): string;
  currencySymbol(code: string): string;
}

export interface PCPStatic {
  /** Merge config and fetch tenant white-label branding. */
  init(overrides?: Partial<PCPConfig>): Promise<PCPConfig>;
  config(): PCPConfig;
  mount(name: 'auction-listing', selector: string | Element, props?: AuctionListingProps): MountInstance | null;
  mount(name: 'auction-room', selector: string | Element, props: AuctionRoomProps): MountInstance | null;
  mount(name: string, selector: string | Element, props?: Record<string, unknown>): MountInstance | null;
  unmount(selector: string | Element): void;
  registerWidget(name: string, definition: { mount(el: Element, props: unknown, ctx: unknown): (() => void) | void }): void;
  listWidgets(): string[];
  api(path: string, options?: RequestInit): Promise<unknown>;
  auth: PCPAuth;
  i18n: PCPI18n;
  toast(message: string, type?: 'default' | 'success' | 'error' | 'warning', duration?: number): void;
  version: string;
}

declare global {
  interface Window {
    PCP: PCPStatic;
    PCPConfig?: PCPConfig;
  }
}

declare const PCP: PCPStatic;
export default PCP;
