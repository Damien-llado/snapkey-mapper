import {clic} from '../lib/clic.js';
// Download button of the site. Without Functions (a deploy of docs/ alone), _redirects sends /download to the APK all the same.
export const onRequest = (context) => clic(context, '/download');
