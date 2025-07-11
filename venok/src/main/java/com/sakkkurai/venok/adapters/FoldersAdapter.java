package com.sakkkurai.venok.adapters;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sakkkurai.venok.R;

import java.util.List;

public class FoldersAdapter extends RecyclerView.Adapter<FoldersAdapter.ViewHolder> {

    private final List<Pair<String, String>> folders;
    private final OnFolderDeleteListener deleteListener;

    public FoldersAdapter(List<Pair<String, String>> folders, OnFolderDeleteListener deleteListener) {
        this.folders = folders;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.setup_scanfolder_layout, parent, false);
        return new ViewHolder(view);
    }


    public void updateFolders(List<Pair<String, String>> newFolders) {
        this.folders.clear();
        this.folders.addAll(newFolders);
        notifyDataSetChanged();
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pair<String, String> folder = folders.get(position);
        holder.folderName.setText(folder.first);
        holder.folderPath.setText(folder.second);

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onFolderDelete(folder);
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, folderPath;
        ImageView deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folder_name);
            folderPath = itemView.findViewById(R.id.folder_path);
            deleteButton = itemView.findViewById(R.id.remove_folder);
        }
    }

    public interface OnFolderDeleteListener {
        void onFolderDelete(Pair<String, String> folder);
    }
}
