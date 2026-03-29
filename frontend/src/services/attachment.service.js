import httpClient from '../utils/httpClient';

const ENDPOINT = '/attachments';

export const attachmentService = {
  /**
   * Fetch attachment constraints/info from backend
   */
  getInfo() {
    return httpClient.get(`${ENDPOINT}/info`);
  },

  /**
   * Upload an attachment for an issue
   * @param {number} issueId
   * @param {File} file
   * @param {number} userId
   */
  async uploadIssueAttachment(issueId, file, userId) {
    const url = `${ENDPOINT}/issues/${issueId}`;
    const form = new FormData();
    form.append('file', file, file.name);

    const headers = {
      // Let browser set Content-Type for multipart
      'X-User-Id': userId != null ? String(userId) : undefined,
      'X-File-Name': file.name,
      'X-File-Size': String(file.size),
    };
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }
    if (headers['X-User-Id'] == null) {
      delete headers['X-User-Id'];
    }

    const response = await fetch(`/api${url}`, {
      method: 'POST',
      body: form,
      headers,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error || `Upload failed with status ${response.status}`);
    }

    return response.json();
  },

  /**
   * Upload an attachment for a comment
   * @param {number} commentId
   * @param {File} file
   * @param {number} userId
   */
  async uploadCommentAttachment(commentId, file, userId) {
    const url = `${ENDPOINT}/comments/${commentId}`;
    const form = new FormData();
    form.append('file', file, file.name);

    const headers = {
      // Let browser set Content-Type for multipart
      'X-User-Id': userId != null ? String(userId) : undefined,
      'X-File-Name': file.name,
      'X-File-Size': String(file.size),
    };
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`;
    }
    if (headers['X-User-Id'] == null) {
      delete headers['X-User-Id'];
    }

    const response = await fetch(`/api${url}`, {
      method: 'POST',
      body: form,
      headers,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.error || `Upload failed with status ${response.status}`);
    }

    return response.json();
  },
};

export default attachmentService;
