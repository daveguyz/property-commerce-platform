/**
 * Property Commerce Platform — Gutenberg editor blocks.
 * Registers two blocks that render as placeholders in the editor
 * (the live widgets need the front-end SDK) with sidebar config.
 */
(function (blocks, element, components, blockEditor) {
  const el = element.createElement;
  const { InspectorControls } = blockEditor;
  const { PanelBody, TextControl, SelectControl } = components;

  function placeholder(title, detail) {
    return el('div', {
      style: {
        border: '2px dashed #6366f1', borderRadius: '8px', padding: '24px',
        textAlign: 'center', color: '#374151', background: '#f9fafb',
      },
    },
      el('strong', { style: { display: 'block', marginBottom: '4px' } }, title),
      el('span', { style: { fontSize: '13px', color: '#6b7280' } }, detail)
    );
  }

  blocks.registerBlockType('property-commerce/auction-listing', {
    title: 'PCP — Auction Listing',
    icon: 'hammer',
    category: 'embed',
    attributes: { status: { type: 'string', default: '' } },
    edit: function (props) {
      return [
        el(InspectorControls, { key: 'i' },
          el(PanelBody, { title: 'Listing options' },
            el(SelectControl, {
              label: 'Initial tab',
              value: props.attributes.status,
              options: [
                { label: 'Live (default)', value: '' },
                { label: 'Upcoming', value: 'SCHEDULED' },
                { label: 'Closed', value: 'CLOSED,SETTLED' },
              ],
              onChange: (v) => props.setAttributes({ status: v }),
            })
          )
        ),
        placeholder('🔨 Auction Listing',
          'Live auction grid renders here on the published page'),
      ];
    },
    save: function () { return null; }, // server-rendered
  });

  blocks.registerBlockType('property-commerce/auction-room', {
    title: 'PCP — Auction Room',
    icon: 'megaphone',
    category: 'embed',
    attributes: { lotId: { type: 'string', default: '' } },
    edit: function (props) {
      return [
        el(InspectorControls, { key: 'i' },
          el(PanelBody, { title: 'Room options' },
            el(TextControl, {
              label: 'Lot ID (blank = read ?lot= from URL)',
              value: props.attributes.lotId,
              onChange: (v) => props.setAttributes({ lotId: v }),
            })
          )
        ),
        placeholder('📡 Live Auction Room',
          props.attributes.lotId
            ? 'Lot: ' + props.attributes.lotId
            : 'Lot ID read from ?lot= URL parameter'),
      ];
    },
    save: function () { return null; },
  });
})(window.wp.blocks, window.wp.element, window.wp.components, window.wp.blockEditor);
