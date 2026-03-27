import { useState, useEffect, useMemo, useRef } from 'react';
import { X, Upload, AlertTriangle, ChevronDown, Check, Loader2, Search, ArrowLeft } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';
import { attachmentService } from '../services/attachment.service';
import { authService } from '../services/auth.service';

const normalizeIssueType = (value) => {
   if (!value) return 'BUG';
   const normalized = String(value).trim().toUpperCase();

   if (['BUG', 'FEATURE', 'DOCUMENTATION', 'QUESTION'].includes(normalized)) {
      return normalized;
   }

   const aliases = {
      TASK: 'QUESTION',
      IMPROVEMENT: 'FEATURE'
   };

   return aliases[normalized] || 'BUG';
};

const ISSUE_TYPE_OPTIONS = [
   { value: 'BUG', label: 'Bug', badgeClass: 'bg-rose-200 text-rose-900' },
   { value: 'FEATURE', label: 'Feature', badgeClass: 'bg-emerald-200 text-emerald-900' },
   { value: 'DOCUMENTATION', label: 'Documentation', badgeClass: 'bg-sky-200 text-sky-900' },
   { value: 'QUESTION', label: 'Question', badgeClass: 'bg-amber-200 text-amber-900' }
];

const PRIORITY_OPTIONS = [
   { value: 'HIGH', label: 'High', badgeClass: 'bg-red-200 text-red-900' },
   { value: 'MEDIUM', label: 'Medium', badgeClass: 'bg-yellow-200 text-yellow-900' },
   { value: 'LOW', label: 'Low', badgeClass: 'bg-green-200 text-green-900' },
   { value: 'CRITICAL', label: 'Critical', badgeClass: 'bg-red-700 text-white' },
];

const BADGE_BASE_CLASS = 'inline-flex rounded-full px-2.5 py-1 text-xs font-medium';
const DICEBEAR_BASE_URL = 'https://api.dicebear.com/9.x/glass/svg';

const resolveCurrentUserId = (user) => {
   if (user?.id != null) {
      return user.id;
   }

   const storedUserId = localStorage.getItem('userId');
   if (storedUserId && !Number.isNaN(Number(storedUserId))) {
      return Number(storedUserId);
   }

   const storedUser = localStorage.getItem('user');
   if (!storedUser) {
      return null;
   }

   try {
      const parsedUser = JSON.parse(storedUser);
      return parsedUser?.id ?? null;
   } catch (error) {
      console.warn('Unable to parse localStorage user while resolving userId', error);
      return null;
   }
};

const getUserDisplayName = (targetUser) => targetUser?.username || targetUser?.email || 'Unknown user';

const getUserAvatarUrl = (targetUser) => {
   const seed = encodeURIComponent(`${getUserDisplayName(targetUser)}-${targetUser?.id ?? 'unknown'}`);
   return `${DICEBEAR_BASE_URL}?seed=${seed}&radius=50&size=64`;
};

const revokeAttachmentPreview = (attachment) => {
   if (!attachment?.previewUrl) {
      return;
   }

   if (typeof attachment.previewUrl === 'string' && attachment.previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(attachment.previewUrl);
   }
};

function FormDropdown({
   value,
   options,
   onChange,
   disabled = false,
   loading = false,
   placeholder = 'Select',
}) {
   const [open, setOpen] = useState(false);
   const rootRef = useRef(null);
   const selectedOption = options.find((option) => option.value === value) || null;

   useEffect(() => {
      if (!open) {
         return undefined;
      }

      const handleOutsideClick = (event) => {
         if (rootRef.current && !rootRef.current.contains(event.target)) {
            setOpen(false);
         }
      };

      document.addEventListener('mousedown', handleOutsideClick);
      return () => document.removeEventListener('mousedown', handleOutsideClick);
   }, [open]);

   return (
      <div ref={rootRef} className="relative min-w-0">
         <button
            type="button"
            onClick={() => !disabled && setOpen((prev) => !prev)}
            disabled={disabled}
            className="inline-flex h-12 w-full items-center justify-between gap-2 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
         >
            {selectedOption?.badgeClass ? (
               <span className={`${BADGE_BASE_CLASS} max-w-[160px] truncate ${selectedOption.badgeClass}`}>
                  {selectedOption.label}
               </span>
            ) : (
               <span className="truncate text-left">{selectedOption?.label || placeholder}</span>
            )}
            {loading ? (
               <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
            ) : (
               <ChevronDown className={`h-4 w-4 text-gray-500 transition-transform ${open ? 'rotate-180' : ''}`} />
            )}
         </button>

         {open && !disabled && (
            <div className="absolute left-0 top-[calc(100%+8px)] z-20 w-full rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl">
               {options.map((option) => (
                  <button
                     key={String(option.value)}
                     type="button"
                     onClick={() => {
                        onChange(option.value);
                        setOpen(false);
                     }}
                     className="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-100"
                  >
                     {option.badgeClass ? (
                        <span className={`${BADGE_BASE_CLASS} ${option.badgeClass}`}>{option.label}</span>
                     ) : (
                        <span className="truncate">{option.label}</span>
                     )}
                     {value === option.value && <Check className="h-4 w-4 text-blue-600" />}
                  </button>
               ))}
            </div>
         )}
      </div>
   );
}

function CreateIssue({
   user,
   onLogout,
   onCancel,
   onSuccess,
   onNavigate,
   editingIssue,
   isDarkMode,
   onToggleTheme
}) {
   const isEditMode = !!editingIssue;
   const currentPage = 'issues';
   const [formData, setFormData] = useState({
      title: editingIssue?.title || '',
      type: normalizeIssueType(editingIssue?.type),
      priority: editingIssue?.priority || 'HIGH',
      assigneeId: editingIssue?.assigneeId != null ? String(editingIssue.assigneeId) : '',
      description: editingIssue?.description || '',
   });
   const [assignableUsers, setAssignableUsers] = useState([]);
   const [assignableUsersLoading, setAssignableUsersLoading] = useState(false);
   const [assignableUsersError, setAssignableUsersError] = useState(null);
   const [showAssigneeModal, setShowAssigneeModal] = useState(false);
   const [assigneeSearchQuery, setAssigneeSearchQuery] = useState('');
   const [pendingAssigneeId, setPendingAssigneeId] = useState('');
   const [attachments, setAttachments] = useState([]);
   const [loading, setLoading] = useState(false);
   const [error, setError] = useState(null);
   const [titleError, setTitleError] = useState(null);
   const [fileError, setFileError] = useState(null);
   const [attachInfo, setAttachInfo] = useState({ maxFileSizeMB: 5, allowedExtensions: ['.jpg', '.png'] });
   const [isDragOver, setIsDragOver] = useState(false);

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
            type: normalizeIssueType(editingIssue.type),
            priority: editingIssue.priority || 'HIGH',
            assigneeId: editingIssue.assigneeId != null ? String(editingIssue.assigneeId) : '',
            description: editingIssue.description || '',
         });
      }
   }, [editingIssue]);

   useEffect(() => {
      return () => {
         attachments.forEach(revokeAttachmentPreview);
      };
   }, [attachments]);

   useEffect(() => {
      let mounted = true;
      const currentUserId = resolveCurrentUserId(user);

      if (currentUserId == null) {
         setAssignableUsers([]);
         setAssignableUsersError('Unable to load assignable users. Please login again.');
         return () => { mounted = false; };
      }

      (async () => {
         try {
            setAssignableUsersLoading(true);
            setAssignableUsersError(null);
            const users = await authService.getAssignableUsers(currentUserId);
            if (!mounted) {
               return;
            }
            setAssignableUsers(Array.isArray(users) ? users : []);
         } catch (e) {
            console.warn('Could not fetch assignable users', e);
            if (mounted) {
               setAssignableUsers([]);
               setAssignableUsersError(e.message || 'Failed to load assignable users.');
            }
         } finally {
            if (mounted) {
               setAssignableUsersLoading(false);
            }
         }
      })();

      return () => { mounted = false; };
   }, [user]);

   useEffect(() => {
      if (showAssigneeModal) {
         setPendingAssigneeId(formData.assigneeId || '');
         setAssigneeSearchQuery('');
      }
   }, [formData.assigneeId, showAssigneeModal]);

   useEffect(() => {
      if (!showAssigneeModal) {
         return undefined;
      }

      const handleEscapeKey = (event) => {
         if (event.key === 'Escape') {
            setShowAssigneeModal(false);
         }
      };

      document.addEventListener('keydown', handleEscapeKey);
      return () => document.removeEventListener('keydown', handleEscapeKey);
   }, [showAssigneeModal]);

   useEffect(() => {
      if (!editingIssue || formData.assigneeId || assignableUsers.length === 0) {
         return;
      }

      if (!editingIssue.assigneeUsername) {
         return;
      }

      const matchingAssignee = assignableUsers.find(
         (assignableUser) => assignableUser.username === editingIssue.assigneeUsername
      );

      if (matchingAssignee?.id != null) {
         setFormData((prev) => ({
            ...prev,
            assigneeId: String(matchingAssignee.id)
         }));
      }
   }, [editingIssue, formData.assigneeId, assignableUsers]);

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

   const handleSelectFieldChange = (fieldName, fieldValue) => {
      setFormData((prev) => ({ ...prev, [fieldName]: fieldValue }));
   };

   const selectedAssigneeUser = assignableUsers.find(
      (assignableUser) => String(assignableUser.id) === formData.assigneeId
   ) || null;
   const selectedAssigneeLabel = selectedAssigneeUser
      ? getUserDisplayName(selectedAssigneeUser)
      : 'Unassigned';
   const filteredAssigneeUsers = useMemo(() => {
      const normalizedQuery = assigneeSearchQuery.trim().toLowerCase();
      if (!normalizedQuery) {
         return assignableUsers;
      }

      return assignableUsers.filter((assignableUser) => {
         const username = (assignableUser.username || '').toLowerCase();
         const email = (assignableUser.email || '').toLowerCase();
         return username.includes(normalizedQuery) || email.includes(normalizedQuery);
      });
   }, [assigneeSearchQuery, assignableUsers]);
   const assigneeRowHoverClass = isDarkMode ? 'hover:bg-[#151515]' : 'hover:bg-gray-50';
   const assigneeRowSelectedClass = isDarkMode ? 'bg-[#1f1f1f]' : 'bg-blue-50';
   const assigneeSecondaryTextClass = isDarkMode ? 'text-gray-300' : 'text-gray-500';

const processAttachmentFile = (file) => {
   setError(null);
   setFileError(null);

   if (!file) {
      return;
   }

   const maxBytes = attachInfo.maxFileSizeMB * 1024 * 1024;
   if (file.size > maxBytes) {
      setFileError(`File is too large (Max ${attachInfo.maxFileSizeMB}MB).`);
      return;
   }

   if (file.size <= 0) {
      setFileError('File is empty');
      return;
   }

   const fileName = file.name || '';
   const ext = fileName.includes('.') ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : '';
   const allowed = (attachInfo.allowedExtensions || ['.jpg', '.png']).map(a => {
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
      previewUrl: URL.createObjectURL(file),
   };

   setAttachments((prevAttachments) => {
      prevAttachments.forEach(revokeAttachmentPreview);
      return [attachment];
   });
};

const handleFileUpload = (e) => {
   const file = e.target.files && e.target.files[0];
   processAttachmentFile(file);
   e.target.value = '';
};

const handleAttachmentDragOver = (event) => {
   event.preventDefault();
   event.stopPropagation();
   if (!isDragOver) {
      setIsDragOver(true);
   }
};

const handleAttachmentDragLeave = (event) => {
   event.preventDefault();
   event.stopPropagation();
   if (!event.currentTarget.contains(event.relatedTarget)) {
      setIsDragOver(false);
   }
};

const handleAttachmentDrop = (event) => {
   event.preventDefault();
   event.stopPropagation();
   setIsDragOver(false);
   const droppedFile = event.dataTransfer?.files?.[0];
   processAttachmentFile(droppedFile);
};

const removeAttachment = (id) => {
   setAttachments(prev => {
      const updatedAttachments = prev.filter(att => att.id !== id);
      prev.forEach((attachment) => {
         if (!updatedAttachments.some((nextAttachment) => nextAttachment.id === attachment.id)) {
            revokeAttachmentPreview(attachment);
         }
      });
      return updatedAttachments;
   });
   setFileError(null);
   setIsDragOver(false);
};


const handleSubmit = async (e) => {
   e.preventDefault();
   setError(null);
   const reporterId = resolveCurrentUserId(user);
   
   // Client-side validation
   if (!formData.title || formData.title.trim().length < 10) {
      setError('Title must be at least 10 characters.');
      return;
   }
   if (!formData.description || formData.description.trim().length === 0) {
      setError('Description cannot be empty.');
      return;
   }
   if (!reporterId) {
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
      const allowed = (attachInfo.allowedExtensions || ['.jpg', '.png']).map(a => {
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
      const assigneeId = formData.assigneeId ? Number(formData.assigneeId) : null;
      if (assigneeId != null && Number.isNaN(assigneeId)) {
         throw new Error('Invalid assignee selected.');
      }

      // Create/Update issue data
      const issueData = {
         title: formData.title.trim(),
         description: formData.description.trim(),
         type: normalizeIssueType(formData.type),
         priority: formData.priority,
         assigneeId
      };

      let issueId;

      if (isEditMode) {
         // Update existing issue
         await issueService.update(editingIssue.id, issueData);
         issueId = editingIssue.id;
      } else {
         // Create new issue
         const response = await issueService.create(issueData, reporterId);
         
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
            await attachmentService.uploadIssueAttachment(issueId, file, reporterId);
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
      const message = err.message || (isEditMode ? 'Failed to update issue' : 'Failed to create issue');
      if (message.includes('Reporter not found')) {
         setError('Session expired or invalid. Please logout and login again.');
      } else {
         setError(message);
      }
   } finally {
      setLoading(false);
   }
};

return (
   <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
         {/* Header */}
         <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
            <div className="flex items-center justify-between">
               <div className="flex items-center gap-3">
                  <button
                     type="button"
                     onClick={onCancel}
                     disabled={loading}
                     className="inline-flex h-10 items-center gap-2 rounded-lg border border-gray-300 bg-white px-3 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                     <ArrowLeft className="h-4 w-4" />
                     <span>Back</span>
                  </button>
                  <h1 className="text-2xl font-bold text-gray-900">
                     {isEditMode ? `Edit Issue #${editingIssue.id}` : 'Create New Issue'}
                  </h1>
               </div>
               <div className="flex items-center gap-3">
                  <ThemeToggle isDarkMode={isDarkMode} onToggle={onToggleTheme} />
                  <UserIdentity user={user} />
                  <button
                     onClick={onLogout}
                     className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
                  >
                     Logout
                  </button>
               </div>
            </div>
         </header>

         {/* Form Content */}
         <div className="mx-auto w-full max-w-6xl p-8 pt-28">
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
                     <FormDropdown
                        value={formData.type}
                        options={ISSUE_TYPE_OPTIONS}
                        onChange={(value) => handleSelectFieldChange('type', value)}
                        placeholder="Type"
                     />
                  </div>

                  {/* Priority */}
                  <div>
                     <label htmlFor="priority" className="block text-sm font-medium text-gray-900 mb-2">
                        Priority<span className="text-red-500">*</span>
                     </label>
                     <FormDropdown
                        value={formData.priority}
                        options={PRIORITY_OPTIONS}
                        onChange={(value) => handleSelectFieldChange('priority', value)}
                        placeholder="Priority"
                     />
                  </div>

                  {/* Assignee */}
                  <div>
                     <label className="block text-sm font-medium text-gray-900 mb-2">
                        Assignee
                     </label>
                     <button
                        type="button"
                        onClick={() => setShowAssigneeModal(true)}
                        disabled={assignableUsersLoading}
                        className="inline-flex h-12 w-full items-center justify-between gap-2 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                     >
                        <div className="flex min-w-0 items-center gap-2">
                           {selectedAssigneeUser ? (
                              <div className="h-7 w-7 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
                                 <img
                                    src={getUserAvatarUrl(selectedAssigneeUser)}
                                    alt={`${selectedAssigneeLabel} avatar`}
                                    className="h-full w-full object-cover"
                                    loading="lazy"
                                    referrerPolicy="no-referrer"
                                 />
                              </div>
                           ) : (
                              <div className="flex h-7 w-7 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-[11px] font-semibold text-gray-500">
                                 All
                              </div>
                           )}
                           <span className="truncate text-left">{selectedAssigneeLabel}</span>
                        </div>
                        {assignableUsersLoading ? (
                           <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
                        ) : (
                           <ChevronDown className="h-4 w-4 text-gray-500" />
                        )}
                     </button>
                     {assignableUsersError && (
                        <p className="mt-1 text-xs text-red-600">{assignableUsersError}</p>
                     )}
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
                  {attachments.length === 0 && (
                     <div
                        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                           isDragOver
                              ? 'border-blue-500 bg-blue-50/60'
                              : 'border-gray-300 hover:border-gray-400'
                        }`}
                        onDragEnter={handleAttachmentDragOver}
                        onDragOver={handleAttachmentDragOver}
                        onDragLeave={handleAttachmentDragLeave}
                        onDrop={handleAttachmentDrop}
                     >
                        <input
                           type="file"
                           id="file-upload"
                           onChange={handleFileUpload}
                           className="hidden"
                           accept={(attachInfo.allowedExtensions || ['.jpg', '.png']).join(',')}
                        />
                        <label
                           htmlFor="file-upload"
                           className="cursor-pointer flex flex-col items-center gap-2"
                        >
                           <Upload className={`w-8 h-8 ${isDragOver ? 'text-blue-500' : 'text-gray-400'}`} />
                           <span className="text-sm text-gray-600">
                              Click to upload or drag and drop
                           </span>
                           <span className="text-xs text-gray-500">
                              PNG, JPG up to {attachInfo.maxFileSizeMB} MB
                           </span>
                           <span className="text-xs text-gray-400">Only one file allowed</span>
                        </label>
                     </div>
                  )}

                  {/* Attachment List */}
                  {attachments.length > 0 && (
                     <div className="mt-4 space-y-2">
                        {attachments.map((attachment) => (
                           <div
                              key={attachment.id}
                              className="flex items-center justify-between p-4 bg-white border border-gray-200 rounded-lg"
                           >
                              <div className="flex items-center gap-3">
                                 <div className="w-12 h-12 bg-gray-100 rounded overflow-hidden flex items-center justify-center">
                                    {attachment.previewUrl ? (
                                       <img
                                          src={attachment.previewUrl}
                                          alt={attachment.name}
                                          className="h-full w-full object-cover"
                                       />
                                    ) : (
                                       <Upload className="w-6 h-6 text-gray-400" />
                                    )}
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

	            {showAssigneeModal && (
	               <div
	                  className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4 backdrop-blur-sm"
	                  onClick={() => setShowAssigneeModal(false)}
	               >
	                  <div
	                     className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl"
	                     onClick={(event) => event.stopPropagation()}
	                  >
	                     <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
	                        <h2 className="text-lg font-semibold text-gray-900">Select assignee</h2>
	                        <button
	                           type="button"
	                           onClick={() => setShowAssigneeModal(false)}
	                           className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
	                        >
	                           <X className="h-5 w-5" />
	                        </button>
	                     </div>

	                     <div className="px-5 py-4">
	                        <div className="relative mb-3">
	                           <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
	                           <input
	                              type="text"
	                              value={assigneeSearchQuery}
	                              onChange={(event) => setAssigneeSearchQuery(event.target.value)}
	                              placeholder="Search users..."
	                              className="w-full rounded-xl border border-gray-300 bg-white py-2.5 pl-9 pr-3 text-sm text-gray-700 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
	                           />
	                        </div>

	                        <div className="stable-scroll max-h-80 overflow-y-auto rounded-xl border border-gray-200">
	                           <button
	                              type="button"
	                              onClick={() => setPendingAssigneeId('')}
	                              className={`flex w-full items-center justify-between px-4 py-3 text-left transition-colors ${assigneeRowHoverClass} ${
	                                 pendingAssigneeId === '' ? assigneeRowSelectedClass : ''
	                              }`}
	                           >
	                              <div className="flex items-center gap-3">
	                                 <div className="flex h-8 w-8 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-xs font-semibold text-gray-600">
	                                    All
	                                 </div>
	                                 <div>
	                                    <p className="text-sm font-medium text-gray-900">Unassigned</p>
	                                    <p className={`text-xs ${assigneeSecondaryTextClass}`}>No assignee selected</p>
	                                 </div>
	                              </div>
	                              {pendingAssigneeId === '' && <Check className="h-4 w-4 text-blue-600" />}
	                           </button>

	                           {filteredAssigneeUsers.length === 0 ? (
	                              <div className="px-4 py-6 text-center text-sm text-gray-500">No users found.</div>
	                           ) : (
	                              filteredAssigneeUsers.map((assignableUser) => {
	                                 const optionValue = String(assignableUser.id);
	                                 const isSelected = pendingAssigneeId === optionValue;
	                                 const displayName = getUserDisplayName(assignableUser);

	                                 return (
	                                    <button
	                                       key={assignableUser.id}
	                                       type="button"
	                                       onClick={() => setPendingAssigneeId(optionValue)}
	                                       className={`flex w-full items-center justify-between border-t border-gray-100 px-4 py-3 text-left transition-colors ${assigneeRowHoverClass} ${
	                                          isSelected ? assigneeRowSelectedClass : ''
	                                       }`}
	                                    >
	                                       <div className="flex min-w-0 items-center gap-3">
	                                          <div className="h-8 w-8 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
	                                             <img
	                                                src={getUserAvatarUrl(assignableUser)}
	                                                alt={`${displayName} avatar`}
	                                                className="h-full w-full object-cover"
	                                                loading="lazy"
	                                                referrerPolicy="no-referrer"
	                                             />
	                                          </div>
	                                          <div className="min-w-0">
	                                             <p className="truncate text-sm font-medium text-gray-900">{displayName}</p>
	                                             {assignableUser.username && assignableUser.email && (
	                                                <p className={`truncate text-xs ${assigneeSecondaryTextClass}`}>{assignableUser.email}</p>
	                                             )}
	                                          </div>
	                                       </div>
	                                       {isSelected && <Check className="h-4 w-4 text-blue-600" />}
	                                    </button>
	                                 );
	                              })
	                           )}
	                        </div>
	                     </div>

	                     <div className="flex items-center justify-end gap-3 border-t border-gray-200 px-5 py-3">
	                        <button
	                           type="button"
	                           onClick={() => setShowAssigneeModal(false)}
	                           className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
	                        >
	                           Cancel
	                        </button>
	                        <button
	                           type="button"
	                           onClick={() => {
	                              handleSelectFieldChange('assigneeId', pendingAssigneeId);
	                              setShowAssigneeModal(false);
	                           }}
	                           className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
	                        >
	                           Apply
	                        </button>
	                     </div>
	                  </div>
	               </div>
	            )}
	         </div>
	      </main>
	   </div>
);
}

export default CreateIssue;
