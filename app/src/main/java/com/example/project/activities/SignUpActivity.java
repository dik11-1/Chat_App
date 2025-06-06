package com.example.project.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project.databinding.ActivitySignUpBinding;
import com.example.project.utilities.Constants;
import com.example.project.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodeImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUn.setOnClickListener(v -> {
            if (isValidSignUpDetails()){
                signUp();
            }
        });
        binding.UserImage.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message, Toast.LENGTH_SHORT).show();
    }

    private void signUp() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        loading(false);
                        showToast("Email đã tồn tại.");
                        binding.inputEmail.requestFocus();
                    } else {
                        HashMap<String, Object> user = new HashMap<>();
                        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
                        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                        user.put(Constants.KEY_PASSWORD, binding.inputPass.getText().toString());
                        user.put(Constants.KEY_IMAGE, encodeImage);

                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .add(user)
                                .addOnSuccessListener(documentReference -> {
                                    loading(false);
                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                                    preferenceManager.putString(Constants.KEY_IMAGE, encodeImage);
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    showToast("Đăng ký thành công.");
                                });
                    }
                });
    }

    private String encoImage(Bitmap image){
        int preWidth = 150;
        int preHeight = image.getHeight() * preWidth/ image.getWidth();
        Bitmap preImage = Bitmap.createScaledBitmap(image, preWidth, preHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        preImage.compress(Bitmap.CompressFormat.JPEG, 50,byteArrayOutputStream);
        byte[] bytes =  byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.imageuser.setVisibility(View.GONE);
                            encodeImage = encoImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }

            }
    );

    private Boolean isValidSignUpDetails(){
        if(encodeImage == null){
            showToast("Hãy chọn hình ảnh đại diện ");
            return false;
        }else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Hãy nhập tên của bạn");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Hãy nhập email đúng định dạng(Vd: example@gmail.com)");
            return false;
        }else if(binding.inputPass.getText().toString().trim().isEmpty()){
            showToast("Hãy nhập mật khẩu!");
            return false;
        }else if(binding.inputPassConfirm.getText().toString().trim().isEmpty()){
            showToast("Hãy xác nhận mật khẩu!");
            return false;
        }else if(!binding.inputPass.getText().toString().equals(binding.inputPassConfirm.getText().toString())){
            binding.inputPass.requestFocus();
            showToast("Sai mật khẩu!");
            return false;
        }else return true;
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUn.setVisibility(View.VISIBLE);
        }
    }

}