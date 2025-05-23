    package com.example.project.activities;
    
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.net.Uri;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.util.Base64;
    import android.util.Log;
    import android.view.KeyEvent;
    import android.view.View;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.PickVisualMediaRequest;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.constraintlayout.widget.ConstraintLayout;

    import com.example.project.adapters.ChatAdapter;
    import com.example.project.databinding.ActivityChatBinding;
    import com.example.project.models.ChatMessage;
    import com.example.project.models.User;
    import com.example.project.utilities.Constants;
    import com.example.project.utilities.PreferenceManager;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.firebase.firestore.DocumentChange;
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.EventListener;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.QuerySnapshot;

    import java.io.ByteArrayOutputStream;
    import java.io.InputStream;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Objects;


    public class ChatActivity extends BaseActivity {
    
        private ActivityChatBinding binding;
        private User receiverUser;
        private List<ChatMessage> chatMessages;
        private ChatAdapter chatAdapter;
        private PreferenceManager preferenceManager;
        private FirebaseFirestore database;
        private String conversionId = null;
        private Boolean isReceive = false;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = ActivityChatBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            setListeners();
            loadReceiverDetail();
            init();
            listenMessages();
        }

        private void init(){
            preferenceManager = new PreferenceManager(getApplicationContext());
            chatMessages = new ArrayList<>();
            chatAdapter = new ChatAdapter(
                    chatMessages,
                    getBitmapFromEncodedString(receiverUser.image),
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
            binding.chatRecycleView.setAdapter(chatAdapter);
            database = FirebaseFirestore.getInstance();

        }
    
        private void sendMessage(){
            if(!binding.inputMessage.getText().toString().isBlank()) {
                HashMap<String, Object> message = new HashMap<>();
                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                message.put(Constants.KEY_CHAT_CONVERSION_ID, conversionId);
                message.put(Constants.KEY_TIMESTAMP, new Date());
                database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                if (conversionId != null) {
                    updateConversion(binding.inputMessage.getText().toString());
                } else {
                    HashMap<String, Object> conversion = new HashMap<>();
                    conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                    conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                    conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                    conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                    conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                    conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
                    conversion.put(Constants.KEY_TIMESTAMP, new Date());
                    addConversion(conversion);
                }
            }
            binding.inputMessage.setText(null);
        }

        private void sendImageMessage(String encodedImage) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            message.put(Constants.KEY_IMAGE_SEND, encodedImage); // <-- thêm ảnh
            message.put(Constants.KEY_CHAT_CONVERSION_ID, conversionId);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

            String placeholder = "[image]";

            if (conversionId != null) {
                updateConversion(placeholder);
            } else {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                conversion.put(Constants.KEY_LAST_MESSAGE, placeholder); // <-- Ghi dạng "[image]"
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                addConversion(conversion);
            }
        }

        private void listenMessages(){
            database.collection(Constants.KEY_COLLECTION_CHAT)
                    .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                    .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                    .addSnapshotListener(eventListener);
            database.collection(Constants.KEY_COLLECTION_CHAT)
                    .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                    .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                    .addSnapshotListener(eventListener);
        }
    
        private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
          if(error != null) {
              return;
          }
          if(value != null) {
              int count = chatMessages.size();
              for (DocumentChange documentChange : value.getDocumentChanges()) {
                  if(documentChange.getType() == DocumentChange.Type.ADDED) {
                      ChatMessage chatMessage = new ChatMessage();
                      chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                      chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                      chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                      chatMessage.image = documentChange.getDocument().getString(Constants.KEY_IMAGE_SEND);
                      chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                      chatMessage.dateOject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                      chatMessages.add(chatMessage);

                  }
              }
              Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateOject.compareTo(obj2.dateOject));
              if(count == 0) {
                  chatAdapter.notifyDataSetChanged();
              }else {
                  chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                  binding.chatRecycleView.smoothScrollToPosition(chatMessages.size() - 1);
              }
              binding.chatRecycleView.setVisibility(View.VISIBLE);
          }
          binding.progressBar.setVisibility(View.GONE);
          if (conversionId == null){
              checkForConversion();
          }
        };
    
        private Bitmap getBitmapFromEncodedString(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    
        }
    
        private void loadReceiverDetail(){
            receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
            binding.textName.setText(receiverUser.name);
        }
    
        private void setListeners() {
            binding.imageBack.setOnClickListener(v -> onBackPressed());
            binding.layoutSend.setOnClickListener(v -> sendMessage());


            binding.inputMessage.setOnEditorActionListener((v, actionId, event) -> {
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();  // Gửi tin nhắn
                    return true;    // Đánh dấu sự kiện đã xử lý
                }
                return false;
            });

            binding.inputMessage.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.inputMessage.getLayoutParams();

                    if (s.length() > 0) {
                        // Đang có nội dung: ẩn layoutSendImage, mở rộng EditText
                        if (binding.layoutSendImage.getVisibility() == View.VISIBLE) {
                            binding.layoutSendImage.setVisibility(View.GONE);
                            params.endToStart = binding.layoutSend.getId();
                            binding.inputMessage.setLayoutParams(params);
                        }
                    } else {
                        // Không còn nội dung: hiện lại layoutSendImage, thu nhỏ EditText
                        if (binding.layoutSendImage.getVisibility() == View.GONE) {
                            binding.layoutSendImage.setVisibility(View.VISIBLE);
                            params.endToStart = binding.layoutSendImage.getId();
                            binding.inputMessage.setLayoutParams(params);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
                    registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                        if (!uris.isEmpty()) {
                            for (Uri uri : uris) {
                                try {
                                    InputStream inputStream = getContentResolver().openInputStream(uri);
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream); // nén ảnh để giảm dung lượng
                                    byte[] imageBytes = outputStream.toByteArray();
                                    String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                                    // Gửi ảnh như một tin nhắn
                                    sendImageMessage(encodedImage);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            Log.d("PhotoPicker", "No media selected");
                        }
                    });
            binding.layoutSendImage.setOnClickListener(v -> {
                pickMultipleMedia.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                );

            });

        }
        private String getReadableDateTime(Date date){
            return new SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()).format(date);
        }
        private void addConversion(HashMap<String, Object> conversion){
            database.collection(Constants.KEY_COLLECTTION_CONVERSATIONS)
                    .add(conversion)
                    .addOnSuccessListener(documentReference -> conversionId =documentReference.getId());
    
        }
    
        private void  updateConversion(String message){
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTTION_CONVERSATIONS)
                            .document(conversionId);
            documentReference.update(
                    Constants.KEY_LAST_MESSAGE, message,
                    Constants.KEY_TIMESTAMP, new Date()
            );
        }
    private void checkForConversion(){
            if (chatMessages.size() != 0){
                checkForConversionRemotely(
                        preferenceManager.getString(Constants.KEY_USER_ID),
                        receiverUser.id
                );
                checkForConversionRemotely(
                        receiverUser.id,
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
            }
    }
    //Hàm lấy conversionID theo senderId và receiverId (Kiểm tra conversion)
        private void checkForConversionRemotely(String senderId, String receiverId){
            database.collection(Constants.KEY_COLLECTTION_CONVERSATIONS)
                    .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                    .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                    .get()
                    .addOnCompleteListener(conversionOncompleteListener);
    
        }
        private final OnCompleteListener<QuerySnapshot> conversionOncompleteListener = task -> {
           if(task.isSuccessful() && task.getResult() !=null && task.getResult().getDocuments().size()> 0){
               DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
               conversionId = documentSnapshot.getId();
           }
        };

    }