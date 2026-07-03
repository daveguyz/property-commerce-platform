/**
 * Property Commerce Platform — pcp-tools VS Code extension.
 *
 * Features:
 *  1. Auction Monitor  — TreeView of live lots, refreshed every 10s
 *  2. API Explorer     — TreeView of platform endpoints; click inserts
 *                        a ready-to-run fetch() snippet at the cursor
 *  3. Snippets         — pcp-init / pcp-auction-room / etc (declarative)
 *
 * Config (settings.json): pcp.apiUrl, pcp.apiKey, pcp.tenantId
 */
import * as vscode from 'vscode';
import * as https from 'https';
import * as http from 'http';

// ─────────────────────────────────────────────────────────────────────
// Platform API helper (uses X-Api-Key — the Phase C gateway path)
// ─────────────────────────────────────────────────────────────────────

interface Lot {
  id: string;
  title: string;
  status: string;
  currentBidAmount?: number;
  startingPrice: number;
  currency: string;
  totalBids: number;
  scheduledEndsAt?: string;
}

function cfg() {
  const c = vscode.workspace.getConfiguration('pcp');
  return {
    apiUrl: c.get<string>('apiUrl', 'https://api.propertycommerce.io'),
    apiKey: c.get<string>('apiKey', ''),
    tenantId: c.get<string>('tenantId', ''),
  };
}

function apiGet<T>(path: string): Promise<T> {
  const { apiUrl, apiKey, tenantId } = cfg();
  return new Promise((resolve, reject) => {
    const url = new URL(apiUrl.replace(/\/$/, '') + path);
    const lib = url.protocol === 'https:' ? https : http;
    const req = lib.get(
      url,
      {
        headers: {
          'X-Api-Key': apiKey,
          'X-Tenant-Id': tenantId,
          Accept: 'application/json',
        },
        timeout: 10_000,
      },
      (res) => {
        let body = '';
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => {
          try {
            const parsed = JSON.parse(body);
            if (res.statusCode && res.statusCode >= 400) {
              reject(new Error(parsed.message || `HTTP ${res.statusCode}`));
            } else {
              resolve(parsed);
            }
          } catch (e) {
            reject(new Error('Invalid JSON from platform'));
          }
        });
      }
    );
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('Request timeout')); });
  });
}

// ─────────────────────────────────────────────────────────────────────
// 1. Auction Monitor TreeView
// ─────────────────────────────────────────────────────────────────────

class LotItem extends vscode.TreeItem {
  constructor(lot: Lot) {
    const price = lot.currentBidAmount ?? lot.startingPrice;
    super(`${lot.title}`, vscode.TreeItemCollapsibleState.None);
    this.description = `${lot.status} · ${lot.currency} ${price.toLocaleString()} · ${lot.totalBids} bids`;
    this.tooltip = new vscode.MarkdownString(
      `**${lot.title}**\n\n` +
      `Status: \`${lot.status}\`\n\n` +
      `Current: ${lot.currency} ${price.toLocaleString()}\n\n` +
      `Bids: ${lot.totalBids}\n\n` +
      (lot.scheduledEndsAt ? `Ends: ${new Date(lot.scheduledEndsAt).toLocaleString()}` : '')
    );
    this.iconPath = new vscode.ThemeIcon(
      lot.status === 'OPEN' || lot.status === 'EXTENDED' ? 'broadcast' : 'calendar'
    );
    this.contextValue = 'pcpLot';
    this.id = lot.id;
  }
}

class AuctionMonitorProvider implements vscode.TreeDataProvider<LotItem> {
  private _onDidChange = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this._onDidChange.event;
  private timer: ReturnType<typeof setInterval> | undefined;

  refresh(): void { this._onDidChange.fire(); }

  startPolling(): void {
    this.timer = setInterval(() => this.refresh(), 10_000);
  }
  stopPolling(): void {
    if (this.timer) clearInterval(this.timer);
  }

  getTreeItem(el: LotItem): vscode.TreeItem { return el; }

  async getChildren(): Promise<LotItem[]> {
    if (!cfg().apiKey) {
      const item = new vscode.TreeItem('Set pcp.apiKey in settings to connect');
      return [item as LotItem];
    }
    try {
      const res = await apiGet<{ data: { content: Lot[] } }>(
        '/api/v1/auctions?status=OPEN,EXTENDED,SCHEDULED&size=25');
      const lots = res.data?.content ?? [];
      if (!lots.length) {
        return [new vscode.TreeItem('No active auctions') as LotItem];
      }
      return lots.map((l) => new LotItem(l));
    } catch (e) {
      return [new vscode.TreeItem(`Error: ${(e as Error).message}`) as LotItem];
    }
  }
}

// ─────────────────────────────────────────────────────────────────────
// 2. API Explorer TreeView
// ─────────────────────────────────────────────────────────────────────

interface Endpoint { method: string; path: string; summary: string; body?: string }

const API_CATALOGUE: Record<string, Endpoint[]> = {
  'Auction Lots': [
    { method: 'GET',  path: '/api/v1/auctions?status=OPEN,EXTENDED', summary: 'List live lots' },
    { method: 'GET',  path: '/api/v1/auctions/{lotId}', summary: 'Get lot detail' },
    { method: 'POST', path: '/api/v1/auctions', summary: 'Create lot',
      body: `{ propertyId: '...', auctionType: 'ENGLISH', startingPrice: 450000, currency: 'USD' }` },
  ],
  'Auction Controls': [
    { method: 'POST', path: '/api/v1/auctions/{lotId}/extend', summary: 'Extend timer', body: `{ extraMinutes: 5 }` },
    { method: 'POST', path: '/api/v1/auctions/{lotId}/pause',  summary: 'Pause auction' },
    { method: 'POST', path: '/api/v1/auctions/{lotId}/close',  summary: 'Close now' },
  ],
  'Q&A': [
    { method: 'POST', path: '/api/v1/auctions/{lotId}/questions', summary: 'Submit question',
      body: `{ content: 'Is VAT included?', category: 'GENERAL' }` },
    { method: 'GET',  path: '/api/v1/auctions/{lotId}/questions/public', summary: 'Public answers' },
  ],
  'Agreements': [
    { method: 'GET',  path: '/api/v1/agreements?lotId={lotId}', summary: 'Agreement for a lot' },
    { method: 'POST', path: '/api/v1/agreements/sign/buyer?token={token}', summary: 'Buyer e-sign',
      body: `{ signatureData: '...' }` },
  ],
  'Webhooks': [
    { method: 'POST', path: '/api/v1/webhooks', summary: 'Register endpoint',
      body: `{ url: 'https://yoursite.com/hook', eventFilter: 'auction.*' }` },
    { method: 'GET',  path: '/api/v1/webhooks', summary: 'List endpoints' },
  ],
  'Tenant': [
    { method: 'GET',  path: '/api/v1/tenants/{tenantId}/config', summary: 'White-label config (public)' },
    { method: 'POST', path: '/api/v1/tenants/{tenantId}/api-keys', summary: 'Issue API key',
      body: `{ label: 'CI pipeline', test: false }` },
  ],
};

type ExplorerNode = { kind: 'group'; name: string } | { kind: 'ep'; ep: Endpoint };

class ApiExplorerProvider implements vscode.TreeDataProvider<ExplorerNode> {
  getTreeItem(node: ExplorerNode): vscode.TreeItem {
    if (node.kind === 'group') {
      return new vscode.TreeItem(node.name, vscode.TreeItemCollapsibleState.Collapsed);
    }
    const item = new vscode.TreeItem(`${node.ep.method} ${node.ep.path}`);
    item.description = node.ep.summary;
    item.iconPath = new vscode.ThemeIcon(node.ep.method === 'GET' ? 'arrow-down' : 'arrow-up');
    item.command = {
      command: 'pcp.insertSnippet',
      title: 'Insert fetch snippet',
      arguments: [node.ep],
    };
    return item;
  }

  getChildren(node?: ExplorerNode): ExplorerNode[] {
    if (!node) {
      return Object.keys(API_CATALOGUE).map((name) => ({ kind: 'group', name }));
    }
    if (node.kind === 'group') {
      return API_CATALOGUE[node.name].map((ep) => ({ kind: 'ep', ep }));
    }
    return [];
  }
}

function fetchSnippet(ep: Endpoint): string {
  const { apiUrl } = cfg();
  const lines = [
    `const res = await fetch('${apiUrl}${ep.path}', {`,
    `  method: '${ep.method}',`,
    `  headers: {`,
    `    'Content-Type': 'application/json',`,
    `    'X-Api-Key': process.env.PCP_API_KEY,`,
    `    'X-Tenant-Id': process.env.PCP_TENANT_ID,`,
    `  },`,
  ];
  if (ep.body) lines.push(`  body: JSON.stringify(${ep.body}),`);
  lines.push(`});`, `const data = await res.json();`);
  return lines.join('\n');
}

// ─────────────────────────────────────────────────────────────────────
// Activation
// ─────────────────────────────────────────────────────────────────────

export function activate(context: vscode.ExtensionContext): void {
  const monitor = new AuctionMonitorProvider();
  monitor.startPolling();

  context.subscriptions.push(
    vscode.window.registerTreeDataProvider('pcpAuctionMonitor', monitor),
    vscode.window.registerTreeDataProvider('pcpApiExplorer', new ApiExplorerProvider()),

    vscode.commands.registerCommand('pcp.refreshMonitor', () => monitor.refresh()),

    vscode.commands.registerCommand('pcp.insertSnippet', (ep: Endpoint) => {
      const editor = vscode.window.activeTextEditor;
      if (!editor) {
        vscode.window.showWarningMessage('Open a file to insert the snippet.');
        return;
      }
      editor.insertSnippet(new vscode.SnippetString(fetchSnippet(ep)));
    }),

    vscode.commands.registerCommand('pcp.openDashboard', () => {
      vscode.env.openExternal(vscode.Uri.parse('https://app.propertycommerce.io'));
    }),

    { dispose: () => monitor.stopPolling() },
  );
}

export function deactivate(): void { /* disposables handle cleanup */ }
