package com.example.project.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.databinding.ItemContainerUserBinding;
import com.example.project.listeners.UserListener;
import com.example.project.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHoder>{

    private final List<User> originalUsers;
    private final List<User> users;
    private final UserListener userListener;


    public UserAdapter(List<User> users, UserListener userListener) {
        this.originalUsers = new ArrayList<>(users);
        this.users = users;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserViewHoder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHoder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHoder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHoder extends RecyclerView.ViewHolder {

        ItemContainerUserBinding binding;

        UserViewHoder(ItemContainerUserBinding itemContainerUserBinding){
            super(itemContainerUserBinding.getRoot());
            binding =  itemContainerUserBinding;
        }
        void setUserData(User user){
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    public void filter(String query) {
        users.clear();
        if (query == null || query.trim().isEmpty()) {
            users.addAll(originalUsers);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User user : originalUsers) {
                if (user.name != null && user.name.toLowerCase().contains(lowerQuery) || user.email != null && user.email.toLowerCase().contains(lowerQuery)){
                    users.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }
}
