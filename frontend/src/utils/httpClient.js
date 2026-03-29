import { API_BASE_URL } from '../constants/api';

/**
 * Generic HTTP client with error handling
 * Wraps fetch API with common configuration
 */
class HttpClient {
  constructor(baseURL = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  /**
   * Make an HTTP request
   * @param {string} endpoint - API endpoint (relative to baseURL)
   * @param {object} options - Fetch options
   * @returns {Promise<any>} Response data
   */
  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;

    // Extract headers separately to avoid being overwritten by spread
    const { headers: customHeaders, ...restOptions } = options;
    const hasBody = Object.prototype.hasOwnProperty.call(restOptions, 'body');

    const headers = {
      ...customHeaders,
    };

    const storedAccessToken = localStorage.getItem('accessToken');
    if (
      storedAccessToken &&
      !headers.Authorization &&
      !headers.authorization
    ) {
      headers.Authorization = `Bearer ${storedAccessToken}`;
    }

    if (!headers['Content-Type'] && !headers['content-type'] && hasBody) {
      headers['Content-Type'] = 'application/json';
    }

    const config = {
      ...restOptions,
      headers,
    };

    try {
      const response = await fetch(url, config);
      const contentType = response.headers.get('content-type') || '';
      const isJsonResponse = contentType.includes('application/json');

      if (!response.ok) {
        const errorPayload = isJsonResponse ? await response.json().catch(() => ({})) : {};
        if (response.status === 401) {
          const sessionExpiredMessage = errorPayload.error || errorPayload.message || 'Session expired. Please login again.';
          const hasActiveToken = Boolean(localStorage.getItem('accessToken'));
          if (hasActiveToken && typeof window !== 'undefined') {
            window.dispatchEvent(
              new CustomEvent('auth:unauthorized', {
                detail: { message: sessionExpiredMessage },
              })
            );
          }
          throw new Error(sessionExpiredMessage);
        }
        throw new Error(
          errorPayload.error ||
            errorPayload.message ||
            `HTTP error! status: ${response.status}`
        );
      }

      if (response.status === 204) {
        return null;
      }

      if (!isJsonResponse) {
        return null;
      }

      return response.json();
    } catch (error) {
      console.error(`API Error: ${error.message}`);
      throw error;
    }
  }

  /**
   * GET request
   */
  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  /**
   * POST request
   */
  post(endpoint, data, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * PUT request
   */
  put(endpoint, data, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  /**
   * PATCH request
   */
  patch(endpoint, data, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  /**
   * DELETE request
   */
  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

// Export singleton instance
export const httpClient = new HttpClient();

export default httpClient;
