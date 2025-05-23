package com.example.project.adapters;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.databinding.ItemContainerRecentConversionBinding;
import com.example.project.listeners.ConversionListener;
import com.example.project.models.ChatMessage;
import com.example.project.models.User;
import com.example.project.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();


    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;

    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }


    class ConversionViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerRecentConversionBinding binding;


        ConversionViewHolder(ItemContainerRecentConversionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessage chatMessage) {

            listenAvailabilityOfReceiver(chatMessage.conversionId);

            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);

            if ("[image]".equals(chatMessage.message)) {
                binding.textRecentMessage.setText("Hình ảnh");
            } else {
                binding.textRecentMessage.setText(chatMessage.message);
            }

            if (chatMessage.isNewMessage) {
                binding.textName.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.received_mess));
                binding.textRecentMessage.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.received_mess));
            } else {
                binding.textName.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.second_text));
                binding.textRecentMessage.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.second_text));
            }



            binding.getRoot().setOnClickListener(v -> {
                    chatMessage.isNewMessage = false;
                    notifyItemChanged(getAdapterPosition());


                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;

                conversionListener.onConversionClicked(user);
            });
        }

        private void listenAvailabilityOfReceiver(String conversionId) {
            // Reset về trạng thái mặc định trước khi gắn listener
            binding.onlineIndicator.setVisibility(View.GONE);
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(conversionId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null || !value.exists()) {
                            binding.onlineIndicator.setVisibility(View.GONE);
                            return;
                        }
                        Long availability = value.getLong(Constants.KEY_AVAILABILITY);
                        if (availability != null && availability == 1) {
                            binding.onlineIndicator.setVisibility(View.VISIBLE);
                        } else {
                            binding.onlineIndicator.setVisibility(View.GONE);
                        }
                    });
        }


    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
