package com.stream.app.controllers;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stream.app.AppConstants;
import com.stream.app.entities.Video;
import com.stream.app.payload.CustomMessage;
import com.stream.app.services.VideoService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin("http://localhost:5173/")
public class VideoController {

    private VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    // Upload Video
    @PostMapping
    public ResponseEntity<?> create(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam("description") String description
    ){
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        Video savedVideo = videoService.save(video,file);

        if(savedVideo != null){
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(video);
        }
        else{
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomMessage.builder()
                    .message("Video Not Uploaded")
                    .success(false)
                    .build()
                );
        }
    }

    @GetMapping
    public List<Video> getAllVideos(){
        return videoService.getAll();
    }

    //  Stream Video
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId){

        Video video = videoService.get(videoId);
        
        String contentType = video.getContentType();
        String filePath = video.getFilePath();

        Resource resource = new FileSystemResource(filePath);

        if(contentType==null){
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
    }
    
    //  Stream video in chunks
    @SuppressWarnings("resource")
    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideo(
        @PathVariable String videoId, 
        @RequestHeader(value = "Range", required = false) String range){

            Video video = videoService.get(videoId);
            Path path = Paths.get(video.getFilePath());

            Resource resource = new FileSystemResource(path);
            String contentType = video.getContentType();
            if(contentType == null){
                contentType = "application/octet-stream";
            }

            long fileLength = path.toFile().length();
            if(range==null){
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
            }

            long rangeStart;
            long rangeEnd;

            String[] ranges = range.replace("bytes=", "").split("-");
            rangeStart = Long.parseLong(ranges[0]);
            
            rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;
            // if(ranges.length>1){
            //     rangeEnd = Long.parseLong(ranges[1]);
            // }
            // else{
            //     rangeEnd = fileLength - 1;
            // }

            if(rangeEnd >= fileLength - 1){
                rangeEnd = fileLength - 1;
            }

            InputStream inputStream;
            try{

                inputStream = Files.newInputStream(path);
                inputStream.skip(rangeStart);
                long contentLength = rangeEnd - rangeStart + 1;

                byte[] data = new byte[(int) contentLength];

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add("Content-Range","bytes "+rangeStart+"-"+rangeEnd+"/"+fileLength);
                httpHeaders.add("Cache-Control", "no-cache, no-store, must-revalidate");
                httpHeaders.add("Pragma", "no-cache");
                httpHeaders.add("Expires", "0");
                httpHeaders.add("X-Content-Type-Options", "nosniff");
                httpHeaders.setContentLength(contentLength);

            return ResponseEntity
            .status(HttpStatus.PARTIAL_CONTENT)
            .headers(httpHeaders)
            .contentType(MediaType.parseMediaType(contentType))
            .body(new ByteArrayResource(data));

            }catch(IOException e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
    }
}
