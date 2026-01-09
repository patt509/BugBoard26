import { useState, useEffect } from 'react';
import { X, Upload, AlertTriangle } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { issueService } from '../services/issue.service';
import { attachmentService } from '../services/attachment.service';


function CreateIssue({ user, onLogout, onCancel, onSuccess, editingIssue }) {
   const isEditMode = !!editingIssue;
   const [currentPage] = useState('issues');
   const [formData, setFormData] = useState({
      title: editingIssue?.title || '',
      type: editingIssue?.type || 'Bug',
      priority: editingIssue?.priority || 'HIGH',
      assignee: editingIssue?.assignee || '',
      description: editingIssue?.description || '',
   });
   const [attachments, setAttachments] = useState([]);
   const [loading, setLoading] = useState(false);
   const [error, setError] = useState(null);
   const [titleError, setTitleError] = useState(null);
   const [fileError, setFileError] = useState(null);
   const [attachInfo, setAttachInfo] = useState({ maxFileSizeMB: 5, allowedExtensions: ['.jpg', '.jpeg', '.png'] });

   // Fetch attachment constraints from backend
   useEffect(() => {
      let mounted = true;
      (async () => {
         try {
            const info = await attachmentService.getInfo();
            if (mounted && info) setAttachInfo(info);
         } catch (e) {
            console.warn('Could not fetch attachment info, using defaults', e);
         }
      })();
      return () => { mounted = false; };
   }, []);

   // Initialize form data when editingIssue changes
   useEffect(() => {
      if (editingIssue) {
         setFormData({
            title: editingIssue.title || '',
            type: editingIssue.type || 'Bug',
            priority: editingIssue.priority || 'HIGH',
            assignee: editingIssue.assignee || '',
            description: editingIssue.description || '',
         });
      }
   }, [editingIssue]);

   const handleInputChange = (e) => {
      const { name, value } = e.target;
      setFormData(prev => ({ ...prev, [name]: value }));
      
      // Validazione titolo in tempo reale
      if (name === 'title') {
         if (value.length > 0 && value.length < 10) {
            setTitleError('Title must be at least 10 characters long.');
         } else {
            setTitleError(null);
         }
      }
   };

const handleFileUpload = (e) => {
   setError(null);
   setFileError(null);
   const file = e.target.files && e.target.files[0];
   if (!file) return;

   // Enforce single file
   // Validate size
   const maxBytes = attachInfo.maxFileSizeMB * 1024 * 1024;
   if (file.size > maxBytes) {
      setFileError(`File is too large (Max ${attachInfo.maxFileSizeMB}MB).`);
      return;
   }

   if (file.size <= 0) {
      setFileError('File is empty');
      return;
   }

   // Validate extension - use server-provided allowed extensions when available
   const fileName = file.name || '';
   const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
   const allowed = (attachInfo.allowedExtensions || ['.jpg', '.jpeg', '.png']).map(a => {
      const lower = a.toLowerCase();
      return lower.startsWith('.') ? lower : `.${lower}`;
   });
   if (!allowed.includes(ext)) {
      setFileError(`Invalid file type. Only ${allowed.join(', ')} images are allowed.`);
      return;
   }

   const attachment = {
      id: Math.random().toString(36).substr(2, 9),
      name: file.name,
      size: (file.size / (1024 * 1024)).toFixed(2) + ' MB',
      file,
   };

   // Only keep a single attachment (replace previous)
   setAttachments([attachment]);
};

const removeAttachment = (id) => {
   setAttachments(prev => prev.filter(att => att.id !== id));
   setFileError(null);
};


const handleSubmit = async (e) => {
   e.preventDefault();
   setError(null);
   
   // Client-side validation
   if (!formData.title || formData.title.trim().length < 10) {
      setError('Title must be at least 10 characters.');
      return;
   }
   if (!formData.description || formData.description.trim().length === 0) {
      setError('Description cannot be empty.');
      return;
   }
   if (!user?.id) {
      setError('User not authenticated. Please login again.');
      return;
   }

   // Validate attachment BEFORE creating issue (only for new issues)
   if (!isEditMode && attachments.length > 0) {
      const file = attachments[0].file;
      const maxBytes = attachInfo.maxFileSizeMB * 1024 * 1024;
      
      if (file.size > maxBytes) {
         setError(`File is too large (Max ${attachInfo.maxFileSizeMB}MB). Please remove or replace the attachment.`);
         return;
      }
      
      const fileName = file.name || '';
      const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
      const allowed = (attachInfo.allowedExtensions || ['.jpg', '.jpeg', '.png']).map(a => {
         const lower = a.toLowerCase();
         return lower.startsWith('.') ? lower : `.${lower}`;
      });
      if (!allowed.includes(ext)) {
         setError(`Invalid file type. Only ${allowed.join(', ')} images are allowed. Please remove or replace the attachment.`);
         return;
      }
   }
   
   setLoading(true);

   try {
      // Create/Update issue data
      const issueData = {
         title: formData.title.trim(),
         description: formData.description.trim(),
         priority: formData.priority,
         // Add other fields as needed by backend
      };

      let issueId;

      if (isEditMode) {
         // Update existing issue
         await issueService.update(editingIssue.id, issueData);
         issueId = editingIssue.id;
      } else {
         // Create new issue
         const response = await issueService.create(issueData, user?.id);
         
         // Backend returns {id, message} on success
         issueId = response?.id;
         if (!issueId) {
            throw new Error('No issue ID returned from server');
         }
      }

      // If there's a new attachment, upload it to the backend (only for new issues)
      if (!isEditMode && attachments.length > 0) {
         const file = attachments[0].file;
         try {
            await attachmentService.uploadIssueAttachment(issueId, file, user?.id);
         } catch (uploadErr) {
            // Use backend message when available
            const msg = uploadErr.message || 'Error uploading attachment';
            setError(msg);
            setLoading(false);
            return;
         }
      }

      if (onSuccess) {
         onSuccess({
            id: issueId,
            title: formData.title.trim()
         });
      }
   } catch (err) {
      console.error(isEditMode ? 'Error updating issue:' : 'Error creating issue:', err);
      setError(err.message || (isEditMode ? 'Failed to update issue' : 'Failed to create issue'));
   } finally {
      setLoading(false);
   }
};

return (
   <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={() => { }} />

      <main className="flex-1 overflow-auto">
         {/* Header */}
         <header className="bg-white border-b border-gray-200 px-8 py-4">
            <div className="flex items-center justify-between">
               <div>
                  <h1 className="text-2xl font-bold text-gray-900">
                     {isEditMode ? `Edit Issue #${editingIssue.id}` : 'Create New Issue'}
                  </h1>
               </div>
               <div className="flex items-center gap-4">
                  <span className="text-sm text-gray-600">
                     {user.username || user.email}
                  </span>
                  <button
                     onClick={onLogout}
                     className="text-sm text-gray-600 hover:text-gray-900"
                  >
                     Logout
                  </button>
               </div>
            </div>
         </header>

         {/* Form Content */}
         <div className="p-8 max-w-6xl">
            <form onSubmit={handleSubmit} className="space-y-6">
               {/* Title */}
               <div>
                  <label htmlFor="title" className="block text-sm font-medium text-gray-900 mb-2">
                     Title<span className="text-red-500">*</span>
                  </label>
                  <input
                     type="text"
                     id="title"
                     name="title"
                     value={formData.title}
                     onChange={handleInputChange}
                     className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                        titleError ? 'border-red-500' : 'border-gray-300'
                     }`}
                     placeholder="Brief summary of the issue"
                     required
                  />
                  {titleError && (
                     <p className="mt-1 text-sm text-blue-600">{titleError}</p>
                  )}
               </div>

               {/* Type, Priority, Assignee Row */}
               <div className="grid grid-cols-3 gap-6">
                  {/* Type */}
                  <div>
                     <label htmlFor="type" className="block text-sm font-medium text-gray-900 mb-2">
                        Type<span className="text-red-500">*</span>
                     </label>
                     <select
                        id="type"
                        name="type"
                        value={formData.type}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white appearance-none"
                        required
                     >
                        <option value="Bug">Bug</option>
                        <option value="Feature">Feature</option>
                        <option value="Task">Task</option>
                        <option value="Improvement">Improvement</option>
                     </select>
                  </div>

                  {/* Priority */}
                  <div>
                     <label htmlFor="priority" className="block text-sm font-medium text-gray-900 mb-2">
                        Priority<span className="text-red-500">*</span>
                     </label>
                     <select
                        id="priority"
                        name="priority"
                        value={formData.priority}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white appearance-none"
                        required
                     >
                        <option value="HIGH">High</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="LOW">Low</option>
                        <option value="CRITICAL">Critical</option>
                     </select>
                  </div>

                  {/* Assignee */}
                  <div>
                     <label htmlFor="assignee" className="block text-sm font-medium text-gray-900 mb-2">
                        Assignee
                     </label>
                     <select
                        id="assignee"
                        name="assignee"
                        value={formData.assignee}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white appearance-none"
                     >
                        <option value="">Select assignee...</option>
                        <option value="m.rossi">m.rossi</option>
                        <option value="g.bianchi">g.bianchi</option>
                        <option value="a.verdi">a.verdi</option>
                     </select>
                  </div>
               </div>

               {/* Description */}
               <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-900 mb-2">
                     Description
                  </label>
                  <textarea
                     id="description"
                     name="description"
                     value={formData.description}
                     onChange={handleInputChange}
                     rows={8}
                     className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-vertical"
                     placeholder="Describe the issue in detail..."
                  />
               </div>

               {/* Attachments */}
               <div>
                  <label className="block text-sm font-medium text-gray-900 mb-2">
                     Attachments
                  </label>

                  {/* Upload Area */}
                  <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-gray-400 transition-colors">
                     <input
                        type="file"
                        id="file-upload"
                        onChange={handleFileUpload}
                        className="hidden"
                        accept={(attachInfo.allowedExtensions || ['.jpg', '.jpeg', '.png']).join(',')}
                     />
                     <label
                        htmlFor="file-upload"
                        className="cursor-pointer flex flex-col items-center gap-2"
                     >
                        <Upload className="w-8 h-8 text-gray-400" />
                        <span className="text-sm text-gray-600">
                           Click to upload or drag and drop
                        </span>
                        <span className="text-xs text-gray-500">
                           PNG, JPG up to {attachInfo.maxFileSizeMB} MB
                        </span>
                        <span className="text-xs text-gray-400">Only one file allowed</span>
                     </label>
                  </div>

                  {/* Attachment List */}
                  {attachments.length > 0 && (
                     <div className="mt-4 space-y-2">
                        {attachments.map((attachment) => (
                           <div
                              key={attachment.id}
                              className="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-lg"
                           >
                              <div className="flex items-center gap-3">
                                 <div className="w-12 h-12 bg-gray-100 rounded flex items-center justify-center">
                                    <Upload className="w-6 h-6 text-gray-400" />
                                 </div>
                                 <div>
                                    <p className="text-sm font-medium text-gray-900">
                                       {attachment.name}
                                    </p>
                                    <p className="text-xs text-gray-500">{attachment.size}</p>
                                 </div>
                              </div>
                              <button
                                 type="button"
                                 onClick={() => removeAttachment(attachment.id)}
                                 className="p-1 hover:bg-gray-100 rounded transition-colors"
                              >
                                 <X className="w-5 h-5 text-gray-500" />
                              </button>
                           </div>
                        ))}
                     </div>
                  )}

                  {/* File Size Error - styled like mockup M9 */}
                  {fileError && (
                     <div className="mt-3 flex items-center gap-2 px-4 py-3 bg-red-500 text-white rounded-lg">
                        <AlertTriangle className="w-5 h-5 flex-shrink-0" />
                        <span className="text-sm font-medium">{fileError}</span>
                     </div>
                  )}
               </div>

               {/* Error Message - styled like mockup M11 (service unavailable) */}
               {error && (
                  <div className="flex items-center gap-2 px-4 py-3 bg-red-500 text-white rounded-lg">
                     <div className="flex-shrink-0 w-5 h-5 bg-white rounded-full flex items-center justify-center">
                        <X className="w-3 h-3 text-red-500" />
                     </div>
                     <span className="text-sm font-medium">{error}</span>
                  </div>
               )}

               {/* Action Buttons */}
               <div className="flex justify-end gap-4 pt-4">
                  <button
                     type="button"
                     onClick={onCancel}
                     className="px-6 py-3 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-50 transition-colors"
                     disabled={loading}
                  >
                     Cancel
                  </button>
                  <button
                     type="submit"
                     disabled={loading || titleError || fileError}
                     className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                     {loading 
                        ? (isEditMode ? 'Updating...' : 'Creating...') 
                        : (isEditMode ? 'Update Issue' : 'Create Issue')}
                  </button>
               </div>
            </form>
         </div>
      </main>
   </div>
);
}

export default CreateIssue;
