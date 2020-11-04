.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main PROC
	push ebp
	mov ebp, esp

	push 90
	pop [ebp-12]
	push 10
	pop [ebp-16]
	push [ebp-12]     ;a_val
	push [ebp-16]     ;b_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D0
	mov edx, 0ffffffffh
	jmp _D1
_D0:
	mov edx, 0
_D1:
	idiv ECX
	push EAX

	pop [ebp-20]
	push 3
	pop [ebp-24]
	push [ebp-24]     ;k_val
	pop EBX
	neg EBX
	push EBX

	push [ebp-12]     ;a_val
	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

pop eax	;if
cmp eax, 0
je _L2

	push [ebp-12]     ;a_val
	push 7
	mov edx, 0
	pop ECX
	pop EAX
	imul ECX
	push EAX

	pop EBX
	neg EBX
	push EBX

	push [ebp-20]     ;c_val
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D2
	mov edx, 0ffffffffh
	jmp _D3
_D2:
	mov edx, 0
_D3:
	idiv ECX
	push EAX

	pop [ebp-24]
	push 30
	pop [ebp-28]
	push 20
	pop [ebp-32]
	push 30
	pop [ebp-36]
	push 1
	pop EBX
	neg EBX
	push EBX

	pop [ebp-12]
	push 10
	pop [ebp-32]
jmp _L3
_L2:
	push 0
	pop [ebp-24]
_L3:
	push [ebp-24]     ;k_val
	pop eax ;here is the result
	mov esp, ebp  ; restore ESP; now it points to old EBP
	pop ebp       ; restore old EBP; now ESP is where it was before prologue
	ret
main ENDP
END start
