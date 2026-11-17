import {clic} from '../../lib/clic.js';
// One download link per channel: /get/youtube, /get/reddit, /get/github (fallback in _redirects too).
export const onRequest = (context) => clic(context, '/get/' + String(context.params.canal).slice(0, 30));
