package com.example.socialnetwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import androidx.appcompat.widget.Toolbar;
import de.hdodenhof.circleimageview.CircleImageView;


public class SettingActivity extends AppCompatActivity {
    private Toolbar mToolbar;

    private EditText userName, userProfName,userStatus,userCountry,userGender,userRelation,userDOB;
    private Button UpdateAccountSettingButton;
    private CircleImageView userProfImage;
    private ProgressDialog loadingBar;

    private DatabaseReference SettinguserRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    final static int Gallery_Pick = 1;
    private StorageReference UserProfileImageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mAuth=FirebaseAuth.getInstance();
        currentUserId=mAuth.getCurrentUser().getUid();
        SettinguserRef= FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        mToolbar=findViewById(R.id.setting_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");


        userProfImage=findViewById(R.id.settings_profile_image);
        userName=findViewById(R.id.settings_username);
        userProfName=findViewById(R.id.settings_profile_full_name);
        userStatus=findViewById(R.id.settings_status);
        userCountry=findViewById(R.id.settings_country);
        userGender=findViewById(R.id.settings_gender);
        userRelation=findViewById(R.id.settings_relationship_status);
        userDOB=findViewById(R.id.settings_dob);
        UpdateAccountSettingButton=findViewById(R.id.update_account_settings_button);
        loadingBar=new ProgressDialog(this);

        SettinguserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String myProfileImage=dataSnapshot.child("profileimage").getValue().toString();
                String myUserName=dataSnapshot.child("username").getValue().toString();
                String myProfileName=dataSnapshot.child("fullname").getValue().toString();
                String myProfileStatus=dataSnapshot.child("status").getValue().toString();
                String myDOB=dataSnapshot.child("dob").getValue().toString();
                String myCountry=dataSnapshot.child("country").getValue().toString();
                String myGender=dataSnapshot.child("gender").getValue().toString();
                String myRelationStatus=dataSnapshot.child("relationshipstatus").getValue().toString();

                Picasso.get().load(myProfileImage).placeholder(R.drawable.profile).into(userProfImage);

                userName.setText(myUserName);
                userProfName.setText(myProfileName);
                userStatus.setText(myProfileStatus);
                userDOB.setText(myDOB);
                userCountry.setText(myCountry);
                userGender.setText(myGender);
                userRelation.setText(myRelationStatus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        UpdateAccountSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidateAccountInfo();
            }
        });
        userProfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==Gallery_Pick && resultCode==RESULT_OK && data!=null)
        {
            Uri ImageUri = data.getData();

            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we updating your profile image...");
                loadingBar.setCanceledOnTouchOutside(true);
                loadingBar.show();

                Uri resultUri = result.getUri();

                StorageReference filePath = UserProfileImageRef.child(currentUserId + ".jpg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task)
                    {
                        if(task.isSuccessful()) {

                            Toast.makeText(SettingActivity.this, "Profile Image stored successfully to Firebase storage...", Toast.LENGTH_SHORT).show();

                            Task<Uri> result = task.getResult().getMetadata().getReference().getDownloadUrl();

                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    final String downloadUrl = uri.toString();

                                    SettinguserRef.child("profileimage").setValue(downloadUrl)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Intent selfIntent = new Intent(SettingActivity.this, SettingActivity.class);
                                                        startActivity(selfIntent);

                                                        Toast.makeText(SettingActivity.this, "Profile Image stored to Firebase Database Successfully...", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    } else {
                                                        String message = task.getException().getMessage();
                                                        Toast.makeText(SettingActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });
                                }
                            });
                        }
                    }
                });
            }
            else {
                Toast.makeText(SettingActivity.this, "Error Occured: Image can not be cropped. Try Again.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }

    private void ValidateAccountInfo() {
        String username=userName.getText().toString();
        String profilename=userProfName.getText().toString();
        String status=userStatus.getText().toString();
        String dob=userDOB.getText().toString();
        String country=userCountry.getText().toString();
        String gender=userGender.getText().toString();
        String relation=userRelation.getText().toString();
        loadingBar.setTitle("Profile Image");
        loadingBar.setMessage("Please wait, while we updating your profile image...");
        loadingBar.setCanceledOnTouchOutside(true);
        loadingBar.show();
        UpdateAccountInfo(username,profilename,status,dob,country,gender,relation);

    }

    private void UpdateAccountInfo(String username, String profilename, String status, String dob, String country, String gender, String relation) {
        HashMap userMap=new HashMap();
        userMap.put("username",username);
        userMap.put("fullname",profilename);
        userMap.put("status",status);
        userMap.put("dob",dob);
        userMap.put("country",country);
        userMap.put("gender",gender);
        userMap.put("relationshipstatus",relation);
        SettinguserRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    SendUserToMainActicity();
                    Toast.makeText(SettingActivity.this,"Setting Updated Successful",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(SettingActivity.this,"Error",Toast.LENGTH_SHORT).show();

                }
            }
        });
    }
    private void SendUserToMainActicity(){
        Intent mainIntent=new Intent(SettingActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
