/**
 * Property Commerce Platform SDK — mock.js
 *
 * When PCPConfig.apiUrl is not set, the SDK runs in mock mode so the
 * demo site (Phase F) and SDK playground work without a live backend,
 * and so a developer can preview a widget on their own site before
 * wiring up real credentials.
 */

export function mockHandle(path, options = {}) {
  if (path.includes('/auctions') && !path.includes('/questions')) {
    return Promise.resolve(mockAuctionLots(path));
  }
  if (path.includes('/properties/search') || path.includes('/properties?')) {
    return Promise.resolve(mockProperties());
  }
  return Promise.resolve({ success: true, data: null });
}

function mockAuctionLots(path) {
  const titles = [
    'Oakwood Residences', 'The Commercial Quarter', 'Riverside Business Park',
    'Harbour View Apartments', 'The Merchant Centre', 'Estate at Millfield',
  ];
  const cities = ['London', 'New York', 'Sydney', 'Cape Town', 'Dubai', 'Singapore'];
  const statuses = ['OPEN', 'SCHEDULED', 'OPEN', 'EXTENDED', 'SCHEDULED', 'CLOSED'];

  const allLots = titles.map((title, i) => ({
    id: `mock-lot-${i + 1}`,
    title,
    propertyId: `mock-prop-${i + 1}`,
    status: statuses[i],
    auctionType: ['ENGLISH', 'DUTCH', 'ENGLISH', 'SEALED_BID', 'ENGLISH', 'ENGLISH'][i],
    startingPrice: [450000, 820000, 310000, 590000, 1200000, 275000][i],
    currentBidAmount: statuses[i] === 'OPEN' || statuses[i] === 'EXTENDED'
      ? [475000, 0, 340000, 615000, 0, 0][i] : null,
    winningAmount: statuses[i] === 'CLOSED' ? 290000 : null,
    currency: 'USD',
    uniqueBidders: [4, 0, 2, 7, 0, 0][i],
    totalBids: [11, 0, 5, 19, 0, 3][i],
    startsAt: new Date(Date.now() + (i - 2) * 86400000).toISOString(),
    scheduledEndsAt: new Date(Date.now() + (i % 2 === 0 ? 1 : -1) * 3600000 + 65000).toISOString(),
    location: { city: cities[i] },
  }));

  // Parse the status query param the same way the widget sends it:
  // ?status=OPEN,EXTENDED
  let filtered = allLots;
  const match = path.match(/status=([^&]+)/);
  if (match) {
    const wanted = decodeURIComponent(match[1]).split(',');
    filtered = allLots.filter((lot) => wanted.includes(lot.status));
  }

  return {
    success: true,
    data: { content: filtered, totalElements: filtered.length, totalPages: 1, page: 0, size: 12 },
  };
}

function mockProperties() {
  return {
    success: true,
    data: {
      content: [1, 2, 3, 4, 5, 6].map((i) => ({
        id: `mock-${i}`,
        title: ['Oakwood Residences', 'The Commercial Quarter', 'Riverside Business Park',
          'Harbour View Apartments', 'The Merchant Centre', 'Estate at Millfield'][i - 1],
        location: { city: ['London', 'New York', 'Sydney', 'Cape Town', 'Dubai', 'Singapore'][i - 1] },
        pricing: { currentDynamicRate: [850000, 1200000, 475000, 620000, 980000, 320000][i - 1], currency: 'USD' },
        bedrooms: [2, 3, 4, 1, 3, 2][i - 1],
        maxGuests: [4, 6, 8, 2, 6, 4][i - 1],
        averageRating: [4.9, 4.7, 4.8, 4.6, 4.5, 4.4][i - 1],
        totalReviews: [42, 28, 61, 15, 19, 8][i - 1],
        imageUrls: [],
        status: 'ACTIVE',
      })),
      totalElements: 6,
      page: 0,
      size: 6,
    },
  };
}
